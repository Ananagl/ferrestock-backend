/*
 * Nombre:                 UsuariosIT.java
 * Descripción:            Pruebas de integración y seguridad de la gestión de usuarios (RF-AUT-006, CA-AUT-07) contra PostgreSQL real.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.3.0
 */
package com.ferrestock.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.ferrestock.ApoyoIT;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

class UsuariosIT extends ApoyoIT {

    private String admin;
    private MockHttpSession sesionAdmin;

    @BeforeEach
    void preparar() throws Exception {
        admin = crearUsuario("ADMIN");
        sesionAdmin = sesionDe(admin);
    }

    private String nombreNuevo() {
        return "nuevo." + UUID.randomUUID().toString().substring(0, 8);
    }

    private org.springframework.test.web.servlet.ResultActions crear(String nombre, String rol, String clave)
            throws Exception {
        return mvc.perform(post("/api/v1/usuarios").with(csrf()).session(sesionAdmin)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"nombreUsuario":"%s","nombreCompleto":"Persona Nueva","rol":"%s","contrasenaTemporal":"%s"}"""
                .formatted(nombre, rol, clave)));
    }

    private org.springframework.test.web.servlet.ResultActions estado(Long id, boolean activo, MockHttpSession sesion)
            throws Exception {
        return mvc.perform(patch("/api/v1/usuarios/" + id + "/estado").with(csrf()).session(sesion)
            .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":" + activo + "}"));
    }

    @Test
    @DisplayName("El ADMIN crea un usuario: 201 + Location, contraseña temporal y auditoría")
    void crearUsuario() throws Exception {
        String nombre = nombreNuevo();
        crear(nombre.toUpperCase(), "OPERATIVO", "Temporal2026")
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/v1/usuarios/")))
            .andExpect(jsonPath("$.nombreUsuario").value(nombre))
            .andExpect(jsonPath("$.debeCambiarPassword").value(true))
            .andExpect(jsonPath("$.passwordHash").doesNotExist());

        login(nombre, "Temporal2026", false).andExpect(status().isOk())
            .andExpect(jsonPath("$.debeCambiarPassword").value(true));
        assertThat(eventos("USUARIO_CREADO", nombre)).isEqualTo(1);
    }

    @Test
    @DisplayName("Nombre duplicado → 409 en el campo; datos inválidos → 400 con errores por campo")
    void validaciones() throws Exception {
        String nombre = nombreNuevo();
        crear(nombre, "OPERATIVO", "Temporal2026").andExpect(status().isCreated());
        crear(nombre, "ADMIN", "Temporal2026")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("DUPLICADO"))
            .andExpect(jsonPath("$.errores[0].campo").value("nombreUsuario"));

        crear("con espacio", "OPERATIVO", "corta")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.length()").value(2));
        crear(nombreNuevo(), "SUPERUSUARIO", "Temporal2026").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CA-AUT-07: un usuario desactivado no ingresa y su sesión abierta (y su Recordarme) se cierran")
    void desactivarCierraSesiones() throws Exception {
        String operador = crearUsuario("OPERATIVO");
        var login = login(operador, CLAVE, true).andReturn();
        MockHttpSession sesionOperador = (MockHttpSession) login.getRequest().getSession(false);
        Cookie recuerdame = login.getResponse().getCookie("FSRECUERDAME");
        mvc.perform(get("/api/v1/auth/sesion").session(sesionOperador)).andExpect(status().isOk());

        estado(idDe(operador), false, sesionAdmin).andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));

        mvc.perform(get("/api/v1/auth/sesion").session(sesionOperador))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        mvc.perform(get("/api/v1/auth/sesion").cookie(recuerdame)).andExpect(status().isUnauthorized());
        login(operador, CLAVE, false).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));

        estado(idDe(operador), true, sesionAdmin).andExpect(status().isOk());
        login(operador, CLAVE, false).andExpect(status().isOk());
        assertThat(eventos("USUARIO_DESACTIVADO", operador)).isEqualTo(1);
        assertThat(eventos("USUARIO_ACTIVADO", operador)).isEqualTo(1);
    }

    @Test
    @DisplayName("Cambiar el rol cierra la sesión del afectado para que ingrese con los permisos nuevos")
    void cambiarRolCierraSesion() throws Exception {
        String operador = crearUsuario("OPERATIVO");
        MockHttpSession sesionOperador = sesionDe(operador);

        mvc.perform(put("/api/v1/usuarios/" + idDe(operador)).with(csrf()).session(sesionAdmin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombreCompleto\":\"Ahora Admin\",\"rol\":\"ADMIN\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("ADMIN"));

        mvc.perform(get("/api/v1/auth/sesion").session(sesionOperador)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/usuarios").session(sesionDe(operador))).andExpect(status().isOk());
        assertThat(eventos("ROL_CAMBIADO", operador)).isEqualTo(1);
    }

    @Test
    @DisplayName("El ADMIN no puede desactivarse, cambiarse el rol ni restablecerse la contraseña")
    void operacionesSobreSiMismo() throws Exception {
        Long propio = idDe(admin);
        estado(propio, false, sesionAdmin).andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("OPERACION_SOBRE_SI_MISMO"));
        mvc.perform(put("/api/v1/usuarios/" + propio).with(csrf()).session(sesionAdmin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombreCompleto\":\"Yo\",\"rol\":\"OPERATIVO\"}"))
            .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/usuarios/" + propio + "/restablecimiento").with(csrf()).session(sesionAdmin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"contrasenaTemporal\":\"Temporal2026\"}"))
            .andExpect(status().isConflict());
        mvc.perform(get("/api/v1/auth/sesion").session(sesionAdmin)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Restablecer desbloquea la cuenta, cierra sus sesiones y obliga a cambiar la contraseña")
    void restablecer() throws Exception {
        String operador = crearUsuario("OPERATIVO");
        MockHttpSession sesionOperador = sesionDe(operador);
        for (int i = 0; i < 5; i++) {
            login(operador, "Incorrecta99", false);
        }
        login(operador, CLAVE, false).andExpect(status().isLocked());

        mvc.perform(post("/api/v1/usuarios/" + idDe(operador) + "/restablecimiento").with(csrf()).session(sesionAdmin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"contrasenaTemporal\":\"Temporal2026\"}"))
            .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/auth/sesion").session(sesionOperador)).andExpect(status().isUnauthorized());
        login(operador, CLAVE, false).andExpect(status().isUnauthorized());
        login(operador, "Temporal2026", false).andExpect(status().isOk())
            .andExpect(jsonPath("$.debeCambiarPassword").value(true));
        assertThat(eventos("CONTRASENA_RESTABLECIDA", operador)).isEqualTo(1);
    }

    @Test
    @DisplayName("Listado con filtros, orden de lista blanca y 404 para un id inexistente")
    void listarYConsultar() throws Exception {
        String operador = crearUsuario("OPERATIVO");
        mvc.perform(get("/api/v1/usuarios").session(sesionAdmin).param("q", operador))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(1))
            .andExpect(jsonPath("$.contenido[0].rol").value("OPERATIVO"));
        mvc.perform(get("/api/v1/usuarios").session(sesionAdmin).param("rol", "ADMIN").param("activo", "true")
                .param("orden", "creadoEn,desc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contenido[0].rol").value("ADMIN"));
        mvc.perform(get("/api/v1/usuarios").session(sesionAdmin).param("orden", "passwordHash,asc"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/usuarios/999999").session(sesionAdmin))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    @DisplayName("Seguridad: un OPERATIVO recibe 403 en todos los endpoints de usuarios; sin CSRF también 403")
    void operativoSinPermiso() throws Exception {
        MockHttpSession sesionOperador = sesionDe(crearUsuario("OPERATIVO"));
        mvc.perform(get("/api/v1/usuarios").session(sesionOperador)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/usuarios").with(csrf()).session(sesionOperador)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombreUsuario\":\"intruso\",\"nombreCompleto\":\"X Y\",\"rol\":\"ADMIN\","
                    + "\"contrasenaTemporal\":\"Temporal2026\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/usuarios").session(sesionAdmin).contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuario WHERE nombre_usuario = 'intruso'", Integer.class))
            .isZero();
    }
}
