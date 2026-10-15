/*
 * Nombre:                 ComprasIT.java
 * Descripción:            Pruebas de integración de compras contra PostgreSQL real: CA-COM-01 a 04, CA-COS-01 y 02, validaciones y consulta.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.8.0
 */
package com.ferrestock.compras;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ferrestock.ApoyoIT;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;

class ComprasIT extends ApoyoIT {

    private MockHttpSession admin;
    private MockHttpSession operador;
    private Long proveedor;

    @BeforeEach
    void preparar() throws Exception {
        admin = sesionDe(crearUsuario("ADMIN"));
        operador = sesionDe(crearUsuario("OPERATIVO"));
        proveedor = crearTercero(false, true);
    }

    private ResultActions comprar(MockHttpSession sesion, Long proveedorId, String lineas) throws Exception {
        return enviar(sesion, "/api/v1/compras", "{\"proveedorId\":%d,\"observacion\":\"Factura 123\",\"lineas\":[%s]}"
            .formatted(proveedorId, lineas));
    }

    private static String linea(Long producto, String cantidad, String costo) {
        return "{\"productoId\":%d,\"cantidad\":%s,\"costoUnitario\":\"%s\"}".formatted(producto, cantidad, costo);
    }

    private Long idDeCompra(ResultActions r) throws Exception {
        return ((Number) JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    private ResultActions anular(MockHttpSession sesion, Long compra) throws Exception {
        return enviar(sesion, "/api/v1/compras/" + compra + "/anulacion", "{\"motivo\":\"Factura del proveedor duplicada\"}");
    }

    @Test
    @DisplayName("CA-COM-01 y CA-COS-02: compra de A×10 a 500 y B×6 a 1.000 → A en 10, B en 10, CPP recalculados y 2 movimientos")
    void compraConVariasLineas() throws Exception {
        Long a = crearProducto("900", false);
        Long b = crearProducto("1500", false);
        cargarInventario(admin, b, "4", "800");

        ResultActions r = comprar(operador, proveedor, linea(a, "10", "500") + "," + linea(b, "6", "1000"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.consecutivo").value(org.hamcrest.Matchers.matchesPattern("COM-\\d{6}")))
            .andExpect(jsonPath("$.total").value("11000.00"))
            .andExpect(jsonPath("$.estado").value("REGISTRADA"))
            .andExpect(jsonPath("$.lineas.length()").value(2))
            .andExpect(jsonPath("$.lineas[0].subtotal").value("5000.00"));
        Long compra = idDeCompra(r);

        assertThat(existenciasDe(a)).isEqualByComparingTo("10");
        assertThat(existenciasDe(b)).isEqualByComparingTo("10");
        assertThat(costoDe(a)).isEqualByComparingTo("500");
        assertThat(costoDe(b)).isEqualByComparingTo("920");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM movimiento_inventario WHERE documento_tipo = 'COMPRA' "
            + "AND documento_id = ?", Integer.class, compra)).isEqualTo(2);
        mvc.perform(get("/api/v1/compras/" + compra).session(operador))
            .andExpect(jsonPath("$.lineas.length()").value(2))
            .andExpect(jsonPath("$.proveedorNombre").value("Tercero de prueba"));
        mvc.perform(get("/api/v1/compras").session(operador).param("proveedorId", proveedor.toString()))
            .andExpect(jsonPath("$.totalElementos").value(1))
            .andExpect(jsonPath("$.contenido[0].lineas").doesNotExist());
        assertThat(eventos("COMPRA_REGISTRADA", "Factura")).isZero();
        assertThat(eventos("COMPRA_REGISTRADA", JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.consecutivo")))
            .isEqualTo(1);
    }

    @Test
    @DisplayName("CA-COS-01: 10 a CPP 1.000 + compra de 30 a 1.400 → 40 unidades, CPP 1.300")
    void costoPromedio() throws Exception {
        Long a = crearProducto("2000", false);
        cargarInventario(admin, a, "10", "1000");
        comprar(operador, proveedor, linea(a, "30", "1400")).andExpect(status().isCreated());
        assertThat(existenciasDe(a)).isEqualByComparingTo("40");
        assertThat(costoDe(a)).isEqualByComparingTo("1300");
    }

    @Test
    @DisplayName("CA-COM-02: una línea con cantidad 0 rechaza la compra completa e indica la línea; nada cambia")
    void lineaInvalida() throws Exception {
        Long a = crearProducto("900", false);
        Long b = crearProducto("900", false);
        long siguiente = jdbc.queryForObject("SELECT siguiente FROM consecutivo WHERE tipo_documento = 'COMPRA'", Long.class);
        comprar(operador, proveedor, linea(a, "5", "100") + "," + linea(b, "0", "100"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("CANTIDAD_INVALIDA"))
            .andExpect(jsonPath("$.errores[0].campo").value("lineas[1].cantidad"));
        assertThat(existenciasDe(a)).isEqualByComparingTo("0");
        assertThat(jdbc.queryForObject("SELECT siguiente FROM consecutivo WHERE tipo_documento = 'COMPRA'", Long.class))
            .isEqualTo(siguiente);
    }

    @Test
    @DisplayName("Validaciones: producto repetido, costo negativo, sin líneas, proveedor que solo es cliente, producto inactivo")
    void validaciones() throws Exception {
        Long a = crearProducto("900", false);
        comprar(operador, proveedor, linea(a, "1", "100") + "," + linea(a, "2", "100"))
            .andExpect(jsonPath("$.codigo").value("PRODUCTO_REPETIDO"))
            .andExpect(jsonPath("$.errores[0].campo").value("lineas[1].productoId"));
        comprar(operador, proveedor, linea(a, "1", "-5"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores[0].campo").value("lineas[0].costoUnitario"));
        comprar(operador, proveedor, "").andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores[0].campo").value("lineas"));
        comprar(operador, crearTercero(true, false), linea(a, "1", "100"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("TERCERO_INACTIVO"))
            .andExpect(jsonPath("$.errores[0].campo").value("proveedorId"));
        jdbc.update("UPDATE producto SET estado = 'INACTIVO' WHERE id = ?", a);
        comprar(operador, proveedor, linea(a, "1", "100"))
            .andExpect(jsonPath("$.codigo").value("PRODUCTO_INACTIVO"))
            .andExpect(jsonPath("$.errores[0].campo").value("lineas[0].productoId"));
    }

    @Test
    @DisplayName("CA-COM-03: anular la última entrada de A (25 existencias, CPP anterior 1.000) → 15 y CPP 1.000")
    void anulacionPermitida() throws Exception {
        Long a = crearProducto("3000", false);
        cargarInventario(admin, a, "15", "1000");
        Long compra = idDeCompra(comprar(operador, proveedor, linea(a, "10", "1600")));
        assertThat(costoDe(a)).isEqualByComparingTo("1240");

        anular(admin, compra)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("ANULADA"))
            .andExpect(jsonPath("$.motivoAnulacion").value("Factura del proveedor duplicada"));

        assertThat(existenciasDe(a)).isEqualByComparingTo("15");
        assertThat(costoDe(a)).isEqualByComparingTo("1000");
        mvc.perform(get("/api/v1/productos/" + a + "/movimientos").session(admin))
            .andExpect(jsonPath("$.contenido[0].tipo").value("ANULACION_COMPRA"))
            .andExpect(jsonPath("$.contenido[0].costoPromedioResultante").value("1000.0000"));
        mvc.perform(get("/api/v1/inventario/verificacion").session(admin))
            .andExpect(jsonPath("$[?(@.productoId == %d)]".formatted(a)).isEmpty());
        anular(admin, compra).andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("DOCUMENTO_ANULADO"));
        assertThat(eventos("COMPRA_ANULADA", "Factura del proveedor duplicada")).isPositive();
    }

    @Test
    @DisplayName("CA-COM-04: anulación bloqueada si ya se vendió (existencias insuficientes) o hubo otra entrada; el operativo recibe 403")
    void anulacionBloqueada() throws Exception {
        Long a = crearProducto("3000", false);
        Long compra = idDeCompra(comprar(operador, proveedor, linea(a, "10", "1000")));
        jdbc.update("UPDATE producto SET existencias = 4 WHERE id = ?", a);
        anular(admin, compra)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("EXISTENCIAS_INSUFICIENTES_PARA_ANULAR"));
        jdbc.update("UPDATE producto SET existencias = 10 WHERE id = ?", a);
        assertThat(jdbc.queryForObject("SELECT estado FROM compra WHERE id = ?", String.class, compra)).isEqualTo("REGISTRADA");

        comprar(operador, proveedor, linea(a, "5", "1300")).andExpect(status().isCreated());
        anular(admin, compra)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("COMPRA_CON_MOVIMIENTOS_POSTERIORES"));
        assertThat(existenciasDe(a)).isEqualByComparingTo("15");

        anular(operador, compra).andExpect(status().isForbidden());
        anular(admin, 999999999L).andExpect(status().isNotFound());
    }
}
