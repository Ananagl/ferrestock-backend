/*
 * Nombre:                 ComprobantesIT.java
 * Descripción:            Pruebas de integración de comprobantes: CA-DOC-01 y 03, búsqueda, ajustes solo para ADMIN y auditoría inmodificable (CA-AUD-02).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.10.0
 */
package com.ferrestock.comprobantes;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ferrestock.ApoyoIT;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

class ComprobantesIT extends ApoyoIT {

    @Test
    @DisplayName("CA-DOC-01 y 03: el comprobante de una venta trae cliente, líneas, total, pago, cambio, usuario y la leyenda")
    void comprobanteDeVenta() throws Exception {
        MockHttpSession admin = sesionDe(crearUsuario("ADMIN"));
        String nombreOperador = crearUsuario("OPERATIVO");
        MockHttpSession operador = sesionDe(nombreOperador);
        Long producto = crearProducto("3700", false);
        Long cliente = crearTercero(true, false);
        cargarInventario(admin, producto, "10", "1000");
        String respuesta = enviar(operador, "/api/v1/ventas", "{\"clienteId\":%d,\"lineas\":[{\"productoId\":%d,\"cantidad\":5}],\"pago\":{\"medio\":\"EFECTIVO\",\"valorRecibido\":\"20000\"}}"
            .formatted(cliente, producto)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String consecutivo = JsonPath.read(respuesta, "$.consecutivo");

        mvc.perform(get("/api/v1/comprobantes/VENTA/" + consecutivo).session(operador))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tercero.nombre").value("Tercero de prueba"))
            .andExpect(jsonPath("$.lineas[0].subtotal").value("18500.00"))
            .andExpect(jsonPath("$.total").value("18500.00"))
            .andExpect(jsonPath("$.cambio").value("1500.00"))
            .andExpect(jsonPath("$.usuario").value(nombreOperador))
            .andExpect(jsonPath("$.leyenda").value("Documento simulado — sin validez fiscal — no transmitido a la DIAN"))
            .andExpect(jsonPath("$.negocio.nombre").exists());
        mvc.perform(get("/api/v1/comprobantes").session(operador).param("consecutivo", consecutivo))
            .andExpect(jsonPath("$.totalElementos").value(1))
            .andExpect(jsonPath("$.contenido[0].tipo").value("VENTA"));
        mvc.perform(get("/api/v1/comprobantes/VENTA/VEN-999999").session(operador)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Los comprobantes de ajuste solo los ve el ADMIN (DP-16); la leyenda sale en inglés si se pide")
    void ajusteSoloAdmin() throws Exception {
        MockHttpSession admin = sesionDe(crearUsuario("ADMIN"));
        MockHttpSession operador = sesionDe(crearUsuario("OPERATIVO"));
        Long producto = crearProducto("3700", false);
        cargarInventario(admin, producto, "3", "1000");
        String consecutivo = jdbc.queryForObject("SELECT consecutivo FROM ajuste WHERE producto_id = ?", String.class, producto);
        mvc.perform(get("/api/v1/comprobantes/AJUSTE/" + consecutivo).session(admin).header("Accept-Language", "en"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lineas[0].detalle").value("ENTRADA"))
            .andExpect(jsonPath("$.leyenda").value("Simulated document — no tax validity — not transmitted to the DIAN"));
        mvc.perform(get("/api/v1/comprobantes/AJUSTE/" + consecutivo).session(operador)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/comprobantes").session(operador).param("tipo", "AJUSTE"))
            .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    @DisplayName("CA-AUD-02: no existe operación para editar ni borrar eventos de auditoría")
    void auditoriaInmodificable() throws Exception {
        MockHttpSession admin = sesionDe(crearUsuario("ADMIN"));
        mvc.perform(delete("/api/v1/auditoria/1").with(csrf()).session(admin)).andExpect(status().is4xxClientError());
        mvc.perform(put("/api/v1/auditoria/1").with(csrf()).session(admin)).andExpect(status().is4xxClientError());
        mvc.perform(get("/api/v1/auditoria").session(admin).param("accion", "AJUSTE_REGISTRADO")).andExpect(status().isOk());
    }
}
