/*
 * Nombre:                 DevolucionesIT.java
 * Descripción:            Pruebas de integración de devoluciones contra PostgreSQL real: CA-DEV-01 a 03, CA-COS-03, devolución sin venta, controles de consistencia y bloqueo de la anulación de ventas con devoluciones.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.9.0
 */
package com.ferrestock.devoluciones;

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

class DevolucionesIT extends ApoyoIT {

    private String nombreOperador;
    private MockHttpSession admin;
    private MockHttpSession operador;
    private Long cliente;

    @BeforeEach
    void preparar() throws Exception {
        admin = sesionDe(crearUsuario("ADMIN"));
        nombreOperador = crearUsuario("OPERATIVO");
        operador = sesionDe(nombreOperador);
        cliente = crearTercero(true, false);
    }

    /** Vende {@code cantidad} de A al cliente y devuelve el id de la venta. */
    private Long vender(Long producto, String cantidad) throws Exception {
        ResultActions r = enviar(operador, "/api/v1/ventas", "{\"clienteId\":%d,\"lineas\":[{\"productoId\":%d,\"cantidad\":%s}],\"pago\":{\"medio\":\"EFECTIVO\",\"valorRecibido\":\"9999999\"}}"
            .formatted(cliente, producto, cantidad)).andExpect(status().isCreated());
        return ((Number) JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    private ResultActions devolver(Long clienteId, Long venta, String motivo, String lineas) throws Exception {
        return enviar(operador, "/api/v1/devoluciones", "{\"clienteId\":%d,\"ventaId\":%s,\"motivo\":%s,\"lineas\":[%s]}".formatted(
            clienteId, venta, motivo == null ? "null" : "\"" + motivo + "\"", lineas));
    }

    private static String linea(Long producto, String cantidad, String resultado) {
        return "{\"productoId\":%d,\"cantidad\":%s,\"resultado\":\"%s\"}".formatted(producto, cantidad, resultado);
    }

    @Test
    @DisplayName("CA-DEV-01: compra A×4 (A en 6), devuelve 2 vinculada y ACEPTADA → A en 8, movimiento de entrada y auditoría con quién validó")
    void devolucionAceptada() throws Exception {
        Long a = crearProducto("2500", false);
        cargarInventario(admin, a, "10", "1000");
        Long venta = vender(a, "4");
        assertThat(existenciasDe(a)).isEqualByComparingTo("6");

        ResultActions r = devolver(cliente, venta, "Producto defectuoso", linea(a, "2", "ACEPTADA"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.consecutivo").value(org.hamcrest.Matchers.matchesPattern("DEV-\\d{6}")))
            .andExpect(jsonPath("$.validadaPor").value(nombreOperador))
            .andExpect(jsonPath("$.aceptadas").value(1))
            .andExpect(jsonPath("$.lineas[0].costoUnitario").doesNotExist());
        assertThat(existenciasDe(a)).isEqualByComparingTo("8");
        String consecutivo = JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.consecutivo");
        assertThat(eventos("DEVOLUCION_REGISTRADA", consecutivo + " de ")).isEqualTo(1);
        assertThat(eventos("DEVOLUCION_REGISTRADA", "validadas por " + nombreOperador)).isPositive();
        mvc.perform(get("/api/v1/productos/" + a + "/movimientos").session(admin))
            .andExpect(jsonPath("$.contenido[0].tipo").value("DEVOLUCION"))
            .andExpect(jsonPath("$.contenido[0].saldoResultante").value("8.000"));
        mvc.perform(get("/api/v1/ventas/" + venta + "/devolvible").session(operador))
            .andExpect(jsonPath("$[0].vendida").value("4.000"))
            .andExpect(jsonPath("$[0].devuelta").value("2.000"))
            .andExpect(jsonPath("$[0].disponible").value("2.000"));
        mvc.perform(get("/api/v1/devoluciones").session(operador).param("ventaId", venta.toString()))
            .andExpect(jsonPath("$.totalElementos").value(1))
            .andExpect(jsonPath("$.contenido[0].ventaConsecutivo").exists());
    }

    @Test
    @DisplayName("CA-DEV-02: A×1 ACEPTADA y B×1 RECHAZADA → solo A sube; la rechazada queda registrada sin movimiento")
    void lineaRechazada() throws Exception {
        Long a = crearProducto("2500", false);
        Long b = crearProducto("1800", false);
        cargarInventario(admin, a, "5", "1000");
        cargarInventario(admin, b, "5", "900");
        Long venta = enviarVentaDos(a, b);
        ResultActions r = devolver(cliente, venta, "Producto defectuoso", linea(a, "1", "ACEPTADA") + "," + linea(b, "1", "RECHAZADA"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.aceptadas").value(1))
            .andExpect(jsonPath("$.rechazadas").value(1));
        assertThat(existenciasDe(a)).isEqualByComparingTo("5");
        assertThat(existenciasDe(b)).isEqualByComparingTo("4");
        Long id = ((Number) JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
        mvc.perform(get("/api/v1/devoluciones/" + id).session(admin))
            .andExpect(jsonPath("$.lineas.length()").value(2))
            .andExpect(jsonPath("$.lineas[1].resultado").value("RECHAZADA"))
            .andExpect(jsonPath("$.lineas[1].costoUnitario").doesNotExist())
            .andExpect(jsonPath("$.lineas[0].costoUnitario").value("1000.0000"));
        assertThat(jdbc.queryForObject("SELECT cantidad_devuelta FROM venta_linea WHERE venta_id = ? AND producto_id = ?",
            java.math.BigDecimal.class, venta, b)).isEqualByComparingTo("0");
    }

    private Long enviarVentaDos(Long a, Long b) throws Exception {
        ResultActions r = enviar(operador, "/api/v1/ventas", "{\"clienteId\":%d,\"lineas\":[{\"productoId\":%d,\"cantidad\":1},{\"productoId\":%d,\"cantidad\":1}],\"pago\":{\"medio\":\"EFECTIVO\",\"valorRecibido\":\"9999999\"}}"
            .formatted(cliente, a, b)).andExpect(status().isCreated());
        return ((Number) JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    @Test
    @DisplayName("CA-DEV-03: venta de A×4 con 3 ya devueltas → devolver 2 más se rechaza (acumulado 5 > 4) y nada cambia")
    void superaLoVendido() throws Exception {
        Long a = crearProducto("2500", false);
        cargarInventario(admin, a, "10", "1000");
        Long venta = vender(a, "4");
        devolver(cliente, venta, "Producto defectuoso", linea(a, "3", "ACEPTADA")).andExpect(status().isCreated());
        devolver(cliente, venta, "Producto defectuoso", linea(a, "2", "ACEPTADA"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("DEVOLUCION_EXCEDE_VENDIDO"))
            .andExpect(jsonPath("$.errores[0].campo").value("lineas[0].cantidad"));
        assertThat(existenciasDe(a)).isEqualByComparingTo("9");
        devolver(cliente, venta, "Producto defectuoso", linea(a, "1", "ACEPTADA")).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("CA-COS-03: vendido a CPP 1.000, hoy 10 a CPP 1.200 → devolución de 2 con venta deja 12 y CPP 1.166,6667")
    void costoHistorico() throws Exception {
        Long a = crearProducto("2500", false);
        cargarInventario(admin, a, "10", "1000");
        Long venta = vender(a, "4");
        enviar(admin, "/api/v1/ajustes", "{\"productoId\":%d,\"tipo\":\"ENTRADA\",\"cantidad\":4,\"costoUnitario\":\"1500\",\"motivo\":\"Sobrante en conteo\"}"
            .formatted(a)).andExpect(status().isCreated());
        assertThat(costoDe(a)).isEqualByComparingTo("1200");
        devolver(cliente, venta, "Producto defectuoso", linea(a, "2", "ACEPTADA")).andExpect(status().isCreated());
        assertThat(existenciasDe(a)).isEqualByComparingTo("12");
        assertThat(costoDe(a)).isEqualByComparingTo("1166.6667");
        mvc.perform(get("/api/v1/inventario/verificacion").session(admin))
            .andExpect(jsonPath("$[?(@.productoId == %d)]".formatted(a)).isEmpty());
    }

    @Test
    @DisplayName("Sin venta: entra al CPP vigente sin cambiarlo, acepta productos inactivos y queda auditada como DEVOLUCION_SIN_VENTA")
    void sinVenta() throws Exception {
        Long a = crearProducto("2500", false);
        cargarInventario(admin, a, "10", "1200");
        jdbc.update("UPDATE producto SET estado = 'INACTIVO' WHERE id = ?", a);
        ResultActions r = devolver(cliente, null, "Cliente trae unidad sin factura", linea(a, "1", "ACEPTADA"))
            .andExpect(status().isCreated());
        assertThat(existenciasDe(a)).isEqualByComparingTo("11");
        assertThat(costoDe(a)).isEqualByComparingTo("1200");
        String consecutivo = JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.consecutivo");
        assertThat(eventos("DEVOLUCION_SIN_VENTA", consecutivo)).isEqualTo(1);
    }

    @Test
    @DisplayName("Controles: motivo vacío, venta de otro cliente, producto que no está en la venta, cantidad 0")
    void controles() throws Exception {
        Long a = crearProducto("2500", false);
        Long b = crearProducto("2500", false);
        cargarInventario(admin, a, "10", "1000");
        Long venta = vender(a, "4");
        devolver(cliente, venta, "  ", linea(a, "1", "ACEPTADA"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores[0].campo").value("motivo"));
        devolver(crearTercero(true, false), venta, "Producto defectuoso", linea(a, "1", "ACEPTADA"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("VENTA_NO_CORRESPONDE"))
            .andExpect(jsonPath("$.errores[0].campo").value("ventaId"));
        devolver(cliente, venta, "Producto defectuoso", linea(b, "1", "ACEPTADA"))
            .andExpect(jsonPath("$.codigo").value("VENTA_NO_CORRESPONDE"))
            .andExpect(jsonPath("$.errores[0].campo").value("lineas[0].productoId"));
        devolver(cliente, venta, "Producto defectuoso", linea(a, "0", "ACEPTADA"))
            .andExpect(jsonPath("$.codigo").value("CANTIDAD_INVALIDA"));
        assertThat(existenciasDe(a)).isEqualByComparingTo("6");
    }

    @Test
    @DisplayName("CA-VEN-06 de punta a punta: una venta con una devolución aceptada no se puede anular")
    void ventaConDevolucionNoSeAnula() throws Exception {
        Long a = crearProducto("2500", false);
        cargarInventario(admin, a, "10", "1000");
        Long venta = vender(a, "4");
        devolver(cliente, venta, "Producto defectuoso", linea(a, "1", "ACEPTADA")).andExpect(status().isCreated());
        enviar(admin, "/api/v1/ventas/" + venta + "/anulacion", "{\"motivo\":\"Cliente desistió de la compra\"}")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("VENTA_CON_DEVOLUCIONES"));
    }
}
