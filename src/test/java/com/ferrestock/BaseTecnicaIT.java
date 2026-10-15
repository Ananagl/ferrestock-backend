/*
 * Nombre:                 BaseTecnicaIT.java
 * Descripción:            Pruebas de integración de la base técnica: arranque del contexto, endpoint de salud,
 *                         rechazo de peticiones sin sesión, cookie CSRF y aplicación correcta de los scripts SQL.
 * Fecha de creación:      2026-10-14
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.7.0
 */
package com.ferrestock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verificación de la Fase 3 contra PostgreSQL real.
 * <p>
 * Usa un contexto propio: el post-procesador {@code csrf()} de spring-security-test, que usan las demás pruebas de
 * integración, reemplaza para siempre el repositorio CSRF del contexto compartido por uno basado en la sesión. Si
 * esta clase corriera después en ese contexto, la cookie {@code XSRF-TOKEN} real no se probaría.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresDePrueba.class)
class BaseTecnicaIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("El endpoint de salud responde UP sin exponer detalles internos")
    void saludRespondeUpSinDetalles() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    @DisplayName("Una ruta protegida sin sesión responde 401 (RF-AUT-004)")
    void rutaProtegidaSinSesionResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/productos"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/auth/csrf entrega la cookie XSRF-TOKEN legible por la SPA")
    void csrfEntregaCookie() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
            .andExpect(status().isNoContent())
            .andExpect(cookie().exists("XSRF-TOKEN"))
            .andExpect(cookie().httpOnly("XSRF-TOKEN", false));
    }

    @Test
    @DisplayName("La documentación OpenAPI no es pública fuera del perfil dev")
    void openApiProtegidaEnPerfilTest() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    @DisplayName("Los scripts SQL crean los consecutivos y las categorías iniciales")
    void datosInicialesCargados() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM consecutivo", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM categoria", Integer.class)).isGreaterThanOrEqualTo(7);
    }

    @Test
    @DisplayName("La auditoría es de solo inserción: UPDATE y DELETE se rechazan (RF-AUD-003)")
    void auditoriaInmutable() {
        jdbc.update("INSERT INTO auditoria (nombre_usuario, accion, descripcion) VALUES ('prueba', 'PRUEBA', 'evento')");
        assertThatThrownBy(() -> jdbc.update("UPDATE auditoria SET descripcion = 'alterado'"))
            .hasMessageContaining("solo inserción");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM auditoria"))
            .hasMessageContaining("solo inserción");
    }

    @Test
    @DisplayName("La base de datos impide existencias negativas (RN-01, última barrera)")
    void existenciasNegativasRechazadas() {
        Long categoria = jdbc.queryForObject("SELECT min(id) FROM categoria", Long.class);
        assertThatThrownBy(() -> jdbc.update(
            "INSERT INTO producto (codigo, nombre, categoria_id, unidad_medida, precio_venta, existencias) "
                + "VALUES ('TST-1', 'Producto prueba', ?, 'UNIDAD', 1000, -1)", categoria))
            .hasMessageContaining("existencias");
    }
}
