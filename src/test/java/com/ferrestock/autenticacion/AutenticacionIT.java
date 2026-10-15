/*
 * Nombre:                 AutenticacionIT.java
 * Descripción:            Pruebas de integración y de seguridad de la autenticación contra PostgreSQL real: escenarios CA-AUT-01 a CA-AUT-08, CSRF, fijación de sesión, cambio obligatorio de contraseña e idioma.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.2.0
 */
package com.ferrestock.autenticacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.ferrestock.PostgresDePrueba;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresDePrueba.class)
class AutenticacionIT {

    private static final String CLAVE = "Clave2026segura";

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder codificador;

    /** Crea un usuario con nombre único y devuelve su nombre. */
    private String crearUsuario(String rol, boolean debeCambiar, boolean activo) {
        String nombre = "prueba." + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("INSERT INTO usuario (nombre_usuario, nombre_completo, password_hash, rol, debe_cambiar_password, "
            + "activo) VALUES (?, ?, ?, ?, ?, ?)", nombre, "Usuario de prueba", codificador.encode(CLAVE), rol,
            debeCambiar, activo);
        return nombre;
    }

    private String crearUsuario(String rol) {
        return crearUsuario(rol, false, true);
    }

    private ResultActions login(String usuario, String clave, boolean recordarme) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"usuario":"%s","contrasena":"%s","recordarme":%s}""".formatted(usuario, clave, recordarme)));
    }

    private static MockHttpSession sesion(MvcResult resultado) {
        return (MockHttpSession) resultado.getRequest().getSession(false);
    }

    private int eventos(String usuario, String accion) {
        return jdbc.queryForObject("SELECT count(*) FROM auditoria WHERE nombre_usuario = ? AND accion = ?",
            Integer.class, usuario, accion);
    }

    @Test
    @DisplayName("CA-AUT-01: login válido crea la sesión, devuelve el rol y se audita")
    void loginValido() throws Exception {
        String usuario = crearUsuario("OPERATIVO");
        MvcResult resultado = login(usuario, CLAVE, false)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombreUsuario").value(usuario))
            .andExpect(jsonPath("$.rol").value("OPERATIVO"))
            .andExpect(jsonPath("$.debeCambiarPassword").value(false))
            .andReturn();

        mvc.perform(get("/api/v1/auth/sesion").session(sesion(resultado))).andExpect(status().isOk());
        assertThat(eventos(usuario, "LOGIN_EXITOSO")).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-AUT-02: credenciales inválidas y usuario inexistente reciben el mismo mensaje genérico")
    void credencialesInvalidas() throws Exception {
        String usuario = crearUsuario("OPERATIVO");
        login(usuario, "Incorrecta99", false)
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"))
            .andExpect(jsonPath("$.detail").value("Usuario o contraseña incorrectos."));
        login("no.existe", "Incorrecta99", false)
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value("Usuario o contraseña incorrectos."));
        assertThat(eventos(usuario, "LOGIN_FALLIDO")).isEqualTo(1);
        assertThat(eventos("no.existe", "LOGIN_FALLIDO")).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("CA-AUT-02 (i18n): el mensaje de error llega en inglés con Accept-Language: en")
    void credencialesInvalidasEnIngles() throws Exception {
        mvc.perform(post("/api/v1/auth/login").with(csrf()).header("Accept-Language", "en")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuario\":\"no.existe\",\"contrasena\":\"x\",\"recordarme\":false}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value("Incorrect username or password."));
    }

    @Test
    @DisplayName("CA-AUT-03: con Recordarme, la cookie persistente restaura la sesión sin contraseña y rota")
    void recordarmeRestauraSesion() throws Exception {
        String usuario = crearUsuario("OPERATIVO");
        MvcResult resultado = login(usuario, CLAVE, true)
            .andExpect(status().isOk())
            .andExpect(cookie().exists("FSRECUERDAME"))
            .andExpect(cookie().httpOnly("FSRECUERDAME", true))
            .andExpect(cookie().maxAge("FSRECUERDAME", 7 * 24 * 60 * 60))
            .andReturn();
        Cookie recuerdame = resultado.getResponse().getCookie("FSRECUERDAME");

        // "Cerrar el navegador": nueva petición sin la sesión, solo con la cookie persistente
        MvcResult restaurada = mvc.perform(get("/api/v1/auth/sesion").cookie(recuerdame))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombreUsuario").value(usuario))
            .andExpect(cookie().exists("FSRECUERDAME"))
            .andReturn();
        assertThat(restaurada.getResponse().getCookie("FSRECUERDAME").getValue()).isNotEqualTo(recuerdame.getValue());
    }

    @Test
    @DisplayName("CA-AUT-03: sin Recordarme no se crea la cookie persistente")
    void sinRecordarmeNoHayCookie() throws Exception {
        login(crearUsuario("OPERATIVO"), CLAVE, false)
            .andExpect(status().isOk())
            .andExpect(cookie().doesNotExist("FSRECUERDAME"));
    }

    @Test
    @DisplayName("CA-AUT-04: sin sesión responde 401 en formato ProblemDetail")
    void sinSesion() throws Exception {
        mvc.perform(get("/api/v1/auditoria"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"))
            .andExpect(jsonPath("$.title").value("Sesión requerida"));
    }

    @Test
    @DisplayName("CA-AUT-05: un OPERATIVO recibe 403 en la auditoría y queda auditado; el ADMIN sí accede")
    void operativoSinPermiso() throws Exception {
        String operativo = crearUsuario("OPERATIVO");
        MvcResult sesionOperativo = login(operativo, CLAVE, false).andReturn();
        mvc.perform(get("/api/v1/auditoria").session(sesion(sesionOperativo)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
        assertThat(eventos(operativo, "ACCESO_DENEGADO")).isEqualTo(1);

        MvcResult sesionAdmin = login(crearUsuario("ADMIN"), CLAVE, false).andReturn();
        mvc.perform(get("/api/v1/auditoria").session(sesion(sesionAdmin)).param("q", operativo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contenido[0].nombreUsuario").value(operativo))
            .andExpect(jsonPath("$.tamano").value(20));
    }

    @Test
    @DisplayName("CA-AUT-06: el logout invalida la sesión, la cookie Recordarme y sus tokens, y se audita")
    void logout() throws Exception {
        String usuario = crearUsuario("OPERATIVO");
        MvcResult resultado = login(usuario, CLAVE, true).andReturn();
        MockHttpSession sesion = sesion(resultado);
        Cookie recuerdame = resultado.getResponse().getCookie("FSRECUERDAME");

        mvc.perform(post("/api/v1/auth/logout").with(csrf()).session(sesion).cookie(recuerdame))
            .andExpect(status().isNoContent())
            .andExpect(cookie().maxAge("FSRECUERDAME", 0));

        mvc.perform(get("/api/v1/auth/sesion").cookie(recuerdame)).andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM persistent_logins WHERE username = ?", Integer.class,
            usuario)).isZero();
        assertThat(eventos(usuario, "LOGOUT")).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-AUT-07: un usuario desactivado no puede ingresar y recibe el mensaje genérico")
    void usuarioDesactivado() throws Exception {
        login(crearUsuario("OPERATIVO", false, false), CLAVE, false)
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    @DisplayName("CA-AUT-08: tras 5 fallos la cuenta se bloquea aun con la clave correcta, y al vencer se libera")
    void bloqueoPorIntentos() throws Exception {
        String usuario = crearUsuario("OPERATIVO");
        for (int i = 0; i < 5; i++) {
            login(usuario, "Incorrecta99", false).andExpect(status().isUnauthorized());
        }
        login(usuario, CLAVE, false)
            .andExpect(status().isLocked())
            .andExpect(jsonPath("$.codigo").value("CUENTA_BLOQUEADA"));
        assertThat(eventos(usuario, "CUENTA_BLOQUEADA")).isEqualTo(1);

        jdbc.update("UPDATE usuario SET bloqueado_hasta = now() - interval '1 minute' WHERE nombre_usuario = ?",
            usuario);
        login(usuario, CLAVE, false).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Seguridad: el login sin token CSRF se rechaza con 403")
    void loginSinCsrf() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuario\":\"x\",\"contrasena\":\"y\",\"recordarme\":false}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Seguridad: el id de sesión cambia al autenticar (sin fijación de sesión)")
    void cambiaIdDeSesion() throws Exception {
        MockHttpSession previa = new MockHttpSession();
        String idPrevio = previa.getId();
        MvcResult resultado = mvc.perform(post("/api/v1/auth/login").with(csrf()).session(previa)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuario\":\"%s\",\"contrasena\":\"%s\",\"recordarme\":false}"
                    .formatted(crearUsuario("OPERATIVO"), CLAVE)))
            .andExpect(status().isOk()).andReturn();
        assertThat(resultado.getRequest().getSession(false).getId()).isNotEqualTo(idPrevio);
    }

    @Test
    @DisplayName("Seguridad: datos de login inválidos responden 400 con error por campo")
    void loginConDatosInvalidos() throws Exception {
        mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuario\":\"\",\"contrasena\":\"\",\"recordarme\":false}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("VALIDACION"))
            .andExpect(jsonPath("$.errores.length()").value(2));
        mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{no es json"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("RF-AUT-007: con contraseña temporal solo se usa /auth hasta cambiarla")
    void cambioObligatorio() throws Exception {
        String admin = crearUsuario("ADMIN", true, true);
        MvcResult resultado = login(admin, CLAVE, false)
            .andExpect(jsonPath("$.debeCambiarPassword").value(true)).andReturn();
        MockHttpSession sesion = sesion(resultado);

        mvc.perform(get("/api/v1/auditoria").session(sesion))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("CAMBIO_CONTRASENA_REQUERIDO"));

        mvc.perform(put("/api/v1/auth/contrasena").with(csrf()).session(sesion)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"actual\":\"%s\",\"nueva\":\"corta\",\"confirmacion\":\"corta\"}".formatted(CLAVE)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores[0].campo").value("nueva"));
        mvc.perform(put("/api/v1/auth/contrasena").with(csrf()).session(sesion)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"actual\":\"Otra12345\",\"nueva\":\"Ferreteria2026\",\"confirmacion\":\"Ferreteria2026\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores[0].campo").value("actual"));

        mvc.perform(put("/api/v1/auth/contrasena").with(csrf()).session(sesion)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"actual\":\"%s\",\"nueva\":\"Ferreteria2026\",\"confirmacion\":\"Ferreteria2026\"}"
                    .formatted(CLAVE)))
            .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/auditoria").session(sesion)).andExpect(status().isOk());
        assertThat(eventos(admin, "CONTRASENA_CAMBIADA")).isEqualTo(1);
        login(admin, "Ferreteria2026", false).andExpect(status().isOk());
    }

    @Test
    @DisplayName("RNF-USA-01: el idioma preferido se guarda y se devuelve en la sesión")
    void idiomaPreferido() throws Exception {
        MockHttpSession sesion = sesion(login(crearUsuario("OPERATIVO"), CLAVE, false).andReturn());
        mvc.perform(put("/api/v1/auth/idioma").with(csrf()).session(sesion).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idioma\":\"en\"}"))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/auth/sesion").session(sesion)).andExpect(jsonPath("$.idioma").value("en"));
        mvc.perform(put("/api/v1/auth/idioma").with(csrf()).session(sesion).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idioma\":\"fr\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("RNF-DES-02: el tamaño de página se limita a 100")
    void tamanoDePaginaLimitado() throws Exception {
        MockHttpSession sesion = sesion(login(crearUsuario("ADMIN"), CLAVE, false).andReturn());
        mvc.perform(get("/api/v1/auditoria").session(sesion).param("tamano", "1000"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tamano").value(100));
        mvc.perform(get("/api/v1/auditoria").session(sesion).param("desde", "no-es-fecha"))
            .andExpect(status().isBadRequest());
    }
}
