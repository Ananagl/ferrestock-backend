/*
 * Nombre:                 ApoyoIT.java
 * Descripción:            Base de las pruebas de integración: PostgreSQL real, MockMvc y utilidades para crear usuarios e iniciar sesión.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.8.0
 */
package com.ferrestock;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

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

    /**
     * Crea un producto activo de la primera categoría.
     *
     * @param precio   precio de venta
     * @param fraccion si se vende en fracciones
     * @return id del producto
     */
    protected Long crearProducto(String precio, boolean fraccion) {
        Long categoria = jdbc.queryForObject("SELECT min(id) FROM categoria", Long.class);
        return jdbc.queryForObject("INSERT INTO producto (codigo, nombre, categoria_id, unidad_medida, permite_fraccion, "
            + "precio_venta) VALUES (?, 'Producto de prueba', ?, 'UNIDAD', ?, ?) RETURNING id", Long.class,
            "P-" + UUID.randomUUID().toString().substring(0, 8), categoria, fraccion, new BigDecimal(precio));
    }

    /**
     * Crea un tercero activo con cédula aleatoria.
     *
     * @param cliente   rol de cliente
     * @param proveedor rol de proveedor
     * @return id del tercero
     */
    protected Long crearTercero(boolean cliente, boolean proveedor) {
        String numero = String.valueOf(ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L));
        return jdbc.queryForObject("INSERT INTO tercero (tipo_persona, tipo_documento, numero_documento, nombre, es_cliente, "
            + "es_proveedor) VALUES ('NATURAL', 'CC', ?, 'Tercero de prueba', ?, ?) RETURNING id", Long.class, numero,
            cliente, proveedor);
    }

    /**
     * Carga existencias con un ajuste de entrada real (inventario inicial).
     *
     * @param admin    sesión de un ADMIN
     * @param producto producto
     * @param cantidad cantidad
     * @param costo    costo unitario
     * @throws Exception si el ajuste no responde 201
     */
    protected void cargarInventario(MockHttpSession admin, Long producto, String cantidad, String costo) throws Exception {
        enviar(admin, "/api/v1/ajustes", "{\"productoId\":%d,\"tipo\":\"ENTRADA\",\"cantidad\":%s,\"costoUnitario\":\"%s\",\"motivo\":\"Inventario inicial\"}"
            .formatted(producto, cantidad, costo)).andExpect(MockMvcResultMatchers.status().isCreated());
    }

    /**
     * @param sesion sesión autenticada
     * @param ruta   ruta de la API
     * @param cuerpo JSON
     * @return resultado del POST con CSRF
     * @throws Exception si falla la petición
     */
    protected ResultActions enviar(MockHttpSession sesion, String ruta, String cuerpo) throws Exception {
        return mvc.perform(post(ruta).with(csrf()).session(sesion).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    /** @return existencias actuales del producto */
    protected BigDecimal existenciasDe(Long producto) {
        return jdbc.queryForObject("SELECT existencias FROM producto WHERE id = ?", BigDecimal.class, producto);
    }

    /** @return costo promedio actual del producto */
    protected BigDecimal costoDe(Long producto) {
        return jdbc.queryForObject("SELECT costo_promedio FROM producto WHERE id = ?", BigDecimal.class, producto);
    }
}
