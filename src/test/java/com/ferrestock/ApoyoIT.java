/*
 * Nombre:                 ApoyoIT.java
 * Descripción:            Base de las pruebas de integración: PostgreSQL real, MockMvc y utilidades para crear usuarios e iniciar sesión.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.3.0
 */
package com.ferrestock;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Clase base de las pruebas de integración de los módulos de negocio.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresDePrueba.class)
public abstract class ApoyoIT {

    /** Contraseña de los usuarios creados por las pruebas. */
    protected static final String CLAVE = "Clave2026segura";

    @Autowired protected MockMvc mvc;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected PasswordEncoder codificador;

    /**
     * Crea un usuario activo, sin cambio de contraseña pendiente, con nombre único.
     *
     * @param rol ADMIN u OPERATIVO
     * @return nombre de usuario
     */
    protected String crearUsuario(String rol) {
        String nombre = "prueba." + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("INSERT INTO usuario (nombre_usuario, nombre_completo, password_hash, rol, debe_cambiar_password) "
            + "VALUES (?, ?, ?, ?, FALSE)", nombre, "Usuario de prueba", codificador.encode(CLAVE), rol);
        return nombre;
    }

    /** @return id del usuario con ese nombre */
    protected Long idDe(String nombreUsuario) {
        return jdbc.queryForObject("SELECT id FROM usuario WHERE nombre_usuario = ?", Long.class, nombreUsuario);
    }

    /**
     * @param usuario    nombre de usuario
     * @param clave      contraseña
     * @param recordarme si se pide sesión persistente
     * @return resultado del login
     * @throws Exception si falla la petición
     */
    protected ResultActions login(String usuario, String clave, boolean recordarme) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"usuario\":\"%s\",\"contrasena\":\"%s\",\"recordarme\":%s}".formatted(usuario, clave, recordarme)));
    }

    /**
     * Inicia sesión y devuelve la sesión HTTP.
     *
     * @param usuario nombre de usuario
     * @return sesión autenticada
     * @throws Exception si el login no responde 200
     */
    protected MockHttpSession sesionDe(String usuario) throws Exception {
        return (MockHttpSession) login(usuario, CLAVE, false).andExpect(status().isOk()).andReturn()
            .getRequest().getSession(false);
    }

    /** @return cantidad de eventos de auditoría con esa acción cuya descripción contiene el texto */
    protected int eventos(String accion, String texto) {
        return jdbc.queryForObject("SELECT count(*) FROM auditoria WHERE accion = ? AND descripcion LIKE ?",
            Integer.class, accion, "%" + texto + "%");
    }
}
