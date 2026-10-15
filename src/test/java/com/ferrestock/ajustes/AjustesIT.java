/*
 * Nombre:                 AjustesIT.java
 * Descripción:            Pruebas de integración de los ajustes de inventario contra PostgreSQL real: CA-AJU-01 a 04, inventario inicial, validaciones, atomicidad, historial y concurrencia.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.7.0
 */
package com.ferrestock.ajustes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.ferrestock.ApoyoIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;

class AjustesIT extends ApoyoIT {

    private String nombreAdmin;
    private MockHttpSession admin;
    private MockHttpSession operador;

    @BeforeEach
    void preparar() throws Exception {
        nombreAdmin = crearUsuario("ADMIN");
        admin = sesionDe(nombreAdmin);
        operador = sesionDe(crearUsuario("OPERATIVO"));
    }

    private Long crearProducto(boolean fraccion) {
        Long categoria = jdbc.queryForObject("SELECT min(id) FROM categoria", Long.class);
        return jdbc.queryForObject("INSERT INTO producto (codigo, nombre, categoria_id, unidad_medida, permite_fraccion, "
            + "precio_venta) VALUES (?, 'Producto de ajuste', ?, 'UNIDAD', ?, 1000) RETURNING id", Long.class,
            "AJU-" + UUID.randomUUID().toString().substring(0, 8), categoria, fraccion);
    }

    private ResultActions ajustar(MockHttpSession sesion, Long producto, String tipo, String cantidad, String costo,
                                  String motivo) throws Exception {
        String cuerpo = "{\"productoId\":%d,\"tipo\":\"%s\",\"cantidad\":%s,\"costoUnitario\":%s,\"motivo\":%s}".formatted(
            producto, tipo, cantidad, costo == null ? "null" : "\"" + costo + "\"",
            motivo == null ? "null" : "\"" + motivo + "\"");
        return mvc.perform(post("/api/v1/ajustes").with(csrf()).session(sesion).contentType(MediaType.APPLICATION_JSON)
            .content(cuerpo));
    }

    /** Producto con existencias cargadas como inventario inicial. */
    private Long conExistencias(String cantidad, String costo) throws Exception {
        Long id = crearProducto(false);
        ajustar(admin, id, "ENTRADA", cantidad, costo, "Inventario inicial").andExpect(status().isCreated());
        return id;
    }

    private BigDecimal existencias(Long id) {
        return jdbc.queryForObject("SELECT existencias FROM producto WHERE id = ?", BigDecimal.class, id);
    }

    private int ajustesDe(Long id) {
        return jdbc.queryForObject("SELECT count(*) FROM ajuste WHERE producto_id = ?", Integer.class, id);
    }

    @Test
    @DisplayName("Inventario inicial: un ajuste de entrada fija existencias y CPP, con consecutivo AJU y movimiento en el kardex")
    void inventarioInicial() throws Exception {
        Long id = crearProducto(false);
        ajustar(admin, id, "ENTRADA", "8", "1250.5", "Inventario inicial")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.consecutivo").value(org.hamcrest.Matchers.matchesPattern("AJU-\\d{6}")))
            .andExpect(jsonPath("$.tipo").value("ENTRADA"))
            .andExpect(jsonPath("$.cantidad").value("8.000"))
            .andExpect(jsonPath("$.costoUnitario").value("1250.5000"))
            .andExpect(jsonPath("$.existenciasResultantes").value("8.000"))
            .andExpect(jsonPath("$.costoPromedioResultante").value("1250.5000"))
            .andExpect(jsonPath("$.usuario").value(nombreAdmin));

        assertThat(jdbc.queryForObject("SELECT costo_promedio FROM producto WHERE id = ?", BigDecimal.class, id))
            .isEqualByComparingTo("1250.5");
        mvc.perform(get("/api/v1/productos/" + id + "/movimientos").session(admin))
            .andExpect(jsonPath("$.totalElementos").value(1))
            .andExpect(jsonPath("$.contenido[0].tipo").value("AJUSTE_ENTRADA"))
            .andExpect(jsonPath("$.contenido[0].documentoTipo").value("AJUSTE"));
        assertThat(eventos("AJUSTE_REGISTRADO", "Motivo: Inventario inicial")).isPositive();
    }

    @Test
    @DisplayName("Una segunda entrada pondera el CPP; una salida sale al CPP vigente y no lo cambia (DP-08)")
    void efectoEnCosto() throws Exception {
        Long id = conExistencias("10", "1000");
        ajustar(admin, id, "ENTRADA", "30", "1400", "Conteo físico: sobrante")
            .andExpect(jsonPath("$.costoPromedioResultante").value("1300.0000"));
        ajustar(admin, id, "SALIDA", "5", "99999", "Unidades vencidas")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.costoUnitario").value("1300.0000"))
            .andExpect(jsonPath("$.existenciasResultantes").value("35.000"))
            .andExpect(jsonPath("$.costoPromedioResultante").value("1300.0000"));
        mvc.perform(get("/api/v1/inventario/verificacion").session(admin))
            .andExpect(jsonPath("$[?(@.productoId == %d)]".formatted(id)).isEmpty());
    }

    @Test
    @DisplayName("CA-AJU-01: salida de 1 con motivo sobre 8 existencias → quedan 7 y aparece en el historial con motivo y responsable")
    void ajusteAutorizado() throws Exception {
        Long id = conExistencias("8", "500");
        ajustar(admin, id, "SALIDA", "1", null, "Unidad dañada en bodega").andExpect(status().isCreated());

        assertThat(existencias(id)).isEqualByComparingTo("7");
        mvc.perform(get("/api/v1/ajustes").session(admin).param("productoId", id.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(2))
            .andExpect(jsonPath("$.contenido[0].tipo").value("SALIDA"))
            .andExpect(jsonPath("$.contenido[0].motivo").value("Unidad dañada en bodega"))
            .andExpect(jsonPath("$.contenido[0].usuario").value(nombreAdmin))
            .andExpect(jsonPath("$.contenido[0].productoNombre").value("Producto de ajuste"))
            .andExpect(jsonPath("$.contenido[0].existenciasResultantes").doesNotExist());
        mvc.perform(get("/api/v1/ajustes").session(admin).param("productoId", id.toString()).param("tipo", "ENTRADA"))
            .andExpect(jsonPath("$.totalElementos").value(1))
            .andExpect(jsonPath("$.contenido[0].motivo").value("Inventario inicial"));
        mvc.perform(get("/api/v1/ajustes").session(admin).param("productoId", id.toString()).param("desde", "2000-01-01")
                .param("hasta", "2000-12-31"))
            .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    @DisplayName("CA-AJU-02: el operativo recibe 403 al registrar o consultar ajustes, sin efecto, y el intento queda auditado")
    void ajusteNoAutorizado() throws Exception {
        Long id = conExistencias("5", "100");
        ajustar(operador, id, "SALIDA", "1", null, "Intento del operativo")
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
        mvc.perform(get("/api/v1/ajustes").session(operador)).andExpect(status().isForbidden());

        assertThat(existencias(id)).isEqualByComparingTo("5");
        assertThat(ajustesDe(id)).isEqualTo(1);
        assertThat(eventos("ACCESO_DENEGADO", "POST /api/v1/ajustes")).isPositive();
    }

    @Test
    @DisplayName("CA-AJU-03: motivo vacío, solo espacios o muy corto → 400 en el campo motivo")
    void ajusteSinMotivo() throws Exception {
        Long id = conExistencias("5", "100");
        for (String motivo : new String[] {null, "", "      ", "  ab  "}) {
            ajustar(admin, id, "SALIDA", "1", null, motivo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.errores[0].campo").value("motivo"));
        }
        assertThat(existencias(id)).isEqualByComparingTo("5");
    }

    @Test
    @DisplayName("CA-AJU-04: salida de 3 con 2 existencias → 409 en la cantidad; quedan 2 y no se consume el consecutivo")
    void ajusteQueDejaNegativo() throws Exception {
        Long id = conExistencias("2", "100");
        long siguiente = jdbc.queryForObject("SELECT siguiente FROM consecutivo WHERE tipo_documento = 'AJUSTE'", Long.class);

        ajustar(admin, id, "SALIDA", "3", null, "Merma por rotura")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("EXISTENCIAS_INSUFICIENTES"))
            .andExpect(jsonPath("$.errores[0].campo").value("cantidad"));

        assertThat(existencias(id)).isEqualByComparingTo("2");
        assertThat(ajustesDe(id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT siguiente FROM consecutivo WHERE tipo_documento = 'AJUSTE'", Long.class))
            .isEqualTo(siguiente);
    }

    @Test
    @DisplayName("Validaciones: entrada sin costo, costo negativo, cantidad 0 o fraccionaria, producto inactivo o inexistente")
    void validaciones() throws Exception {
        Long id = crearProducto(false);
        ajustar(admin, id, "ENTRADA", "5", null, "Inventario inicial")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("COSTO_UNITARIO_REQUERIDO"))
            .andExpect(jsonPath("$.errores[0].campo").value("costoUnitario"));
        ajustar(admin, id, "ENTRADA", "5", "-1", "Inventario inicial")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores[0].campo").value("costoUnitario"));
        ajustar(admin, id, "ENTRADA", "0", "10", "Inventario inicial")
            .andExpect(jsonPath("$.codigo").value("CANTIDAD_INVALIDA"));
        ajustar(admin, id, "ENTRADA", "1.5", "10", "Inventario inicial")
            .andExpect(jsonPath("$.codigo").value("CANTIDAD_FRACCIONARIA_NO_PERMITIDA"));
        ajustar(admin, crearProducto(true), "ENTRADA", "1.5", "10", "Inventario inicial").andExpect(status().isCreated());

        jdbc.update("UPDATE producto SET estado = 'INACTIVO' WHERE id = ?", id);
        ajustar(admin, id, "ENTRADA", "5", "10", "Inventario inicial")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("PRODUCTO_INACTIVO"));
        ajustar(admin, 999999999L, "ENTRADA", "5", "10", "Inventario inicial").andExpect(status().isNotFound());
        assertThat(ajustesDe(id)).isZero();
    }

    @Test
    @DisplayName("Concurrencia: 6 ajustes de salida simultáneos de 1 sobre 4 existencias → exactamente 4 confirmados")
    void concurrencia() throws Exception {
        Long id = conExistencias("4", "100");
        int intentos = 6;
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(intentos);
        try {
            List<Future<Integer>> resultados = new ArrayList<>();
            for (int i = 0; i < intentos; i++) {
                Callable<Integer> tarea = () -> {
                    salida.await();
                    return ajustar(admin, id, "SALIDA", "1", null, "Salida concurrente").andReturn().getResponse().getStatus();
                };
                resultados.add(hilos.submit(tarea));
            }
            salida.countDown();
            int confirmados = 0;
            for (Future<Integer> r : resultados) {
                int estado = r.get();
                assertThat(estado).isIn(201, 409);
                confirmados += estado == 201 ? 1 : 0;
            }
            assertThat(confirmados).isEqualTo(4);
        } finally {
            hilos.shutdownNow();
        }
        assertThat(existencias(id)).isEqualByComparingTo("0");
        assertThat(ajustesDe(id)).isEqualTo(5);
    }
}
