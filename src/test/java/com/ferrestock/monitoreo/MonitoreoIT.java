/*
 * Nombre:                 MonitoreoIT.java
 * Descripción:            Pruebas de integración del monitoreo: registro de errores, reporte a Taiga de los recurrentes sin duplicar y métricas solo para ADMIN (RF-MON-005, 006).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.13.0
 */
package com.ferrestock.monitoreo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;

import com.ferrestock.ApoyoIT;
import com.ferrestock.monitoreo.entidad.RegistroError;
import com.ferrestock.monitoreo.servicio.RegistroErrorServicio;
import com.ferrestock.monitoreo.servicio.ReporteTaigaServicio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class MonitoreoIT extends ApoyoIT {

    @Autowired private RegistroErrorServicio registro;
    @MockitoBean private ReporteTaigaServicio taiga;

    @Test
    @DisplayName("RF-MON-006: el mismo error 6 veces en la misma ruta crea un solo issue en Taiga y lo enlaza al registro")
    void erroresRecurrentes() throws Exception {
        when(taiga.crearIssue(anyString(), anyString())).thenReturn(Optional.of("#77"));
        String ruta = "/api/v1/prueba/" + UUID.randomUUID();
        RegistroError ultimo = null;
        for (int i = 0; i < 6; i++) {
            ultimo = registro.registrar(UUID.randomUUID(), "GET", ruta, new IllegalStateException("Falla simulada"), null);
        }
        verify(taiga, times(1)).crearIssue(anyString(), anyString());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM registro_error WHERE ruta = ? AND taiga_ref = '#77'", Integer.class, ruta))
            .isEqualTo(1);
        assertThat(ultimo.getTaigaRef()).isNull();

        MockHttpSession admin = sesionDe(crearUsuario("ADMIN"));
        mvc.perform(get("/api/v1/monitoreo/errores").session(admin).param("tamano", "100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contenido[?(@.ruta == '%s' && @.taigaRef == '#77')]".formatted(ruta)).isNotEmpty());
        mvc.perform(get("/api/v1/monitoreo/metricas").session(admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errores24h").isNumber())
            .andExpect(jsonPath("$.erroresReportadosTaiga").isNumber())
            .andExpect(jsonPath("$.valorInventario").exists());
        mvc.perform(get("/api/v1/monitoreo/metricas").session(sesionDe(crearUsuario("OPERATIVO")))).andExpect(status().isForbidden());
    }
}
