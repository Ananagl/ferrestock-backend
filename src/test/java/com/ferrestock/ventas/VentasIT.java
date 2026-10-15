/*
 * Nombre:                 VentasIT.java
 * Descripción:            Pruebas de integración de ventas y pagos contra PostgreSQL real: CA-VEN-01 a 06, CA-PAG-01 y 02, concurrencia y anulación al costo histórico.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.8.0
 */
package com.ferrestock.ventas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.ferrestock.ApoyoIT;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;

class VentasIT extends ApoyoIT {

    private MockHttpSession admin;
    private MockHttpSession operador;
    private Long cliente;

    @BeforeEach
    void preparar() throws Exception {
        admin = sesionDe(crearUsuario("ADMIN"));
        operador = sesionDe(crearUsuario("OPERATIVO"));
        cliente = crearTercero(true, false);
    }

    private ResultActions vender(MockHttpSession sesion, Long clienteId, String lineas, String medio, String recibido) throws Exception {
        return enviar(sesion, "/api/v1/ventas", "{\"clienteId\":%s,\"lineas\":[%s],\"pago\":{\"medio\":\"%s\",\"valorRecibido\":\"%s\"}}"
            .formatted(clienteId, lineas, medio, recibido));
    }

    private static String linea(Long producto, String cantidad) {
        return "{\"productoId\":%d,\"cantidad\":%s}".formatted(producto, cantidad);
    }

    private static Long id(ResultActions r) throws Exception {
        return ((Number) JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    @Test
    @DisplayName("CA-VEN-01: A×4 a 2.500 con 10.000 en efectivo → total 10.000, cambio 0, A en 6, consecutivo y auditoría")
    void ventaValida() throws Exception {
        Long a = crearProducto("2500", false);
        cargarInventario(admin, a, "10", "1200");
        ResultActions r = vender(operador, cliente, linea(a, "4"), "EFECTIVO", "10000")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.consecutivo").value(org.hamcrest.Matchers.matchesPattern("VEN-\\d{6}")))
            .andExpect(jsonPath("$.total").value("10000.00"))
            .andExpect(jsonPath("$.cambio").value("0.00"))
            .andExpect(jsonPath("$.lineas[0].precioUnitario").value("2500.00"))
            .andExpect(jsonPath("$.lineas[0].costoUnitario").doesNotExist());
        assertThat(existenciasDe(a)).isEqualByComparingTo("6");
        String consecutivo = JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.consecutivo");
        assertThat(eventos("VENTA_REGISTRADA", consecutivo)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT costo_unitario FROM venta_linea WHERE venta_id = ?", java.math.BigDecimal.class,
            id(r))).isEqualByComparingTo("1200");
        mvc.perform(get("/api/v1/ventas/" + id(r)).session(admin)).andExpect(jsonPath("$.lineas[0].costoUnitario").value("1200.0000"));
        mvc.perform(get("/api/v1/ventas").session(operador).param("clienteId", cliente.toString()))
            .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    @DisplayName("CA-PAG-01: total 18.500 pagado con 20.000 → cambio 1.500")
    void pagoConCambio() throws Exception {
        Long a = crearProducto("3700", false);
        cargarInventario(admin, a, "10", "1000");
        vender(operador, cliente, linea(a, "5"), "EFECTIVO", "20000")
            .andExpect(jsonPath("$.total").value("18500.00"))
            .andExpect(jsonPath("$.valorRecibido").value("20000.00"))
            .andExpect(jsonPath("$.cambio").value("1500.00"));
    }

    @Test
    @DisplayName("CA-PAG-02: pago de 15.000 o con TARJETA → se rechaza y la venta no se confirma")
    void pagoInvalido() throws Exception {
        Long a = crearProducto("3700", false);
        cargarInventario(admin, a, "10", "1000");
        vender(operador, cliente, linea(a, "5"), "EFECTIVO", "15000")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("PAGO_INSUFICIENTE"))
            .andExpect(jsonPath("$.errores[0].campo").value("pago.valorRecibido"));
        vender(operador, cliente, linea(a, "5"), "TARJETA", "20000")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("MEDIO_PAGO_NO_PERMITIDO"));
        assertThat(existenciasDe(a)).isEqualByComparingTo("10");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM venta WHERE cliente_id = ?", Integer.class, cliente)).isZero();
    }

    @Test
    @DisplayName("CA-VEN-02: vender A×5 con 3 existencias → 'Existencias insuficientes para A: disponibles 3' y A sigue en 3")
    void masDeLoDisponible() throws Exception {
        Long a = crearProducto("1000", false);
        cargarInventario(admin, a, "3", "500");
        String codigo = jdbc.queryForObject("SELECT codigo FROM producto WHERE id = ?", String.class, a);
        vender(operador, cliente, linea(a, "5"), "EFECTIVO", "99999")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value("Existencias insuficientes para " + codigo + ": disponibles 3."))
            .andExpect(jsonPath("$.errores[0].campo").value("lineas[0].cantidad"));
        assertThat(existenciasDe(a)).isEqualByComparingTo("3");
    }

    @Test
    @DisplayName("CA-VEN-03: dos ventas simultáneas de la última unidad → exactamente una confirmada y A en 0")
    void ventasConcurrentes() throws Exception {
        Long a = crearProducto("1000", false);
        cargarInventario(admin, a, "1", "500");
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> resultados = new ArrayList<>();
            for (MockHttpSession sesion : List.of(operador, admin)) {
                resultados.add(hilos.submit(() -> {
                    salida.await();
                    return vender(sesion, cliente, linea(a, "1"), "EFECTIVO", "1000").andReturn().getResponse().getStatus();
                }));
            }
            salida.countDown();
            List<Integer> estados = new ArrayList<>();
            for (Future<Integer> f : resultados) {
                estados.add(f.get());
            }
            assertThat(estados).containsExactlyInAnyOrder(201, 409);
        } finally {
            hilos.shutdownNow();
        }
        assertThat(existenciasDe(a)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("CA-VEN-04: sin cliente, con cliente inexistente o con un tercero que solo es proveedor → se rechaza")
    void sinCliente() throws Exception {
        Long a = crearProducto("1000", false);
        cargarInventario(admin, a, "5", "500");
        vender(operador, null, linea(a, "1"), "EFECTIVO", "1000")
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores[0].campo").value("clienteId"));
        vender(operador, 999999999L, linea(a, "1"), "EFECTIVO", "1000")
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.errores[0].campo").value("clienteId"));
        vender(operador, crearTercero(false, true), linea(a, "1"), "EFECTIVO", "1000")
            .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("TERCERO_INACTIVO"));
        vender(operador, cliente, linea(a, "1") + "," + linea(a, "2"), "EFECTIVO", "5000")
            .andExpect(jsonPath("$.codigo").value("PRODUCTO_REPETIDO"));
        assertThat(existenciasDe(a)).isEqualByComparingTo("5");
    }

    @Test
    @DisplayName("CA-VEN-05: anular una venta sin devoluciones → ANULADA, A recupera 4 al costo histórico y el consecutivo no se reutiliza")
    void anulacion() throws Exception {
        Long a = crearProducto("2500", false);
        cargarInventario(admin, a, "10", "1000");
        ResultActions r = vender(operador, cliente, linea(a, "4"), "EFECTIVO", "10000");
        Long venta = id(r);
        String consecutivo = JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.consecutivo");
        enviar(admin, "/api/v1/ajustes", "{\"productoId\":%d,\"tipo\":\"ENTRADA\",\"cantidad\":6,\"costoUnitario\":\"2000\",\"motivo\":\"Sobrante en conteo\"}"
            .formatted(a)).andExpect(status().isCreated());
        assertThat(costoDe(a)).isEqualByComparingTo("1500");

        enviar(operador, "/api/v1/ventas/" + venta + "/anulacion", "{\"motivo\":\"Error en la venta\"}").andExpect(status().isForbidden());
        enviar(admin, "/api/v1/ventas/" + venta + "/anulacion", "{\"motivo\":\"Cliente desistió de la compra\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("ANULADA"));
        assertThat(existenciasDe(a)).isEqualByComparingTo("16");
        // (12 × 1.500 + 4 × 1.000) / 16 = 1.375
        assertThat(costoDe(a)).isEqualByComparingTo("1375");
        mvc.perform(get("/api/v1/productos/" + a + "/movimientos").session(admin))
            .andExpect(jsonPath("$.contenido[0].tipo").value("ANULACION_VENTA"))
            .andExpect(jsonPath("$.contenido[0].costoUnitario").value("1000.0000"));
        enviar(admin, "/api/v1/ventas/" + venta + "/anulacion", "{\"motivo\":\"Otra vez\"}")
            .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("DOCUMENTO_ANULADO"));
        String siguiente = JsonPath.read(vender(operador, cliente, linea(a, "1"), "EFECTIVO", "2500").andReturn().getResponse()
            .getContentAsString(), "$.consecutivo");
        assertThat(siguiente).isNotEqualTo(consecutivo);
        mvc.perform(get("/api/v1/inventario/verificacion").session(admin))
            .andExpect(jsonPath("$[?(@.productoId == %d)]".formatted(a)).isEmpty());
    }

    @Test
    @DisplayName("CA-VEN-06: una venta con devoluciones no se anula (VENTA_CON_DEVOLUCIONES)")
    void anulacionConDevoluciones() throws Exception {
        Long a = crearProducto("2500", false);
        cargarInventario(admin, a, "10", "1000");
        Long venta = id(vender(operador, cliente, linea(a, "4"), "EFECTIVO", "10000"));
        jdbc.update("UPDATE venta_linea SET cantidad_devuelta = 1 WHERE venta_id = ?", venta);
        enviar(admin, "/api/v1/ventas/" + venta + "/anulacion", "{\"motivo\":\"Cliente desistió de la compra\"}")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("VENTA_CON_DEVOLUCIONES"));
        assertThat(existenciasDe(a)).isEqualByComparingTo("6");
    }
}
