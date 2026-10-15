/*
 * Nombre:                 InventarioIT.java
 * Descripción:            Pruebas de integración del núcleo de inventario contra PostgreSQL real: kardex (CA-INV-01), existencias no negativas con concurrencia (RN-01), consecutivos sin repetir ni huecos, valorización y verificación.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.6.0
 */
package com.ferrestock.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.ferrestock.ApoyoIT;
import com.ferrestock.catalogo.entidad.Producto;
import com.ferrestock.catalogo.servicio.ProductoServicio;
import com.ferrestock.compartido.documentos.TipoDocumentoComercial;
import com.ferrestock.compartido.error.CodigoError;
import com.ferrestock.compartido.error.NegocioException;
import com.ferrestock.compartido.seguridad.UsuarioAutenticado;
import com.ferrestock.comprobantes.servicio.ConsecutivoServicio;
import com.ferrestock.inventario.entidad.TipoMovimiento;
import com.ferrestock.inventario.servicio.MovimientoInventarioServicio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

class InventarioIT extends ApoyoIT {

    @Autowired private ProductoServicio productos;
    @Autowired private MovimientoInventarioServicio movimientos;
    @Autowired private ConsecutivoServicio consecutivos;
    @Autowired private TransactionTemplate transaccion;

    private Long adminId;
    private String nombreAdmin;
    private MockHttpSession admin;
    private MockHttpSession operador;

    @BeforeEach
    void preparar() throws Exception {
        nombreAdmin = crearUsuario("ADMIN");
        adminId = idDe(nombreAdmin);
        admin = sesionDe(nombreAdmin);
        operador = sesionDe(crearUsuario("OPERATIVO"));
    }

    /** Las operaciones de servicio necesitan un usuario autenticado en el hilo (para el kardex). */
    private void comoAdmin() {
        UsuarioAutenticado u = new UsuarioAutenticado(adminId, "admin", "Admin", "ADMIN", null, true, null, false, "es", "s");
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(u, null,
            u.getAuthorities()));
    }

    private Long crearProducto(boolean fraccion) {
        Long categoria = jdbc.queryForObject("SELECT min(id) FROM categoria", Long.class);
        return jdbc.queryForObject("INSERT INTO producto (codigo, nombre, categoria_id, unidad_medida, permite_fraccion, "
            + "precio_venta) VALUES (?, 'Producto de inventario', ?, 'UNIDAD', ?, 1000) RETURNING id", Long.class,
            "INV-" + UUID.randomUUID().toString().substring(0, 8), categoria, fraccion);
    }

    private void entrada(Long id, String cantidad, String costo) {
        comoAdmin();
        transaccion.executeWithoutResult(e -> {
            Producto p = productos.bloquearParaMovimiento(List.of(id)).get(id);
            movimientos.registrarEntrada(p, new BigDecimal(cantidad), new BigDecimal(costo), TipoMovimiento.AJUSTE_ENTRADA,
                true, TipoDocumentoComercial.AJUSTE, 1L);
        });
    }

    private void salida(Long id, String cantidad, TipoMovimiento tipo) {
        comoAdmin();
        transaccion.executeWithoutResult(e -> {
            Producto p = productos.bloquearParaMovimiento(List.of(id)).get(id);
            movimientos.registrarSalida(p, new BigDecimal(cantidad), tipo, TipoDocumentoComercial.AJUSTE, 1L);
        });
    }

    private BigDecimal existencias(Long id) {
        return jdbc.queryForObject("SELECT existencias FROM producto WHERE id = ?", BigDecimal.class, id);
    }

    @Test
    @DisplayName("CA-INV-01: entrada de 10, venta de 3 y ajuste de salida de 1 → kardex con saldos 10, 7 y 6")
    void kardex() throws Exception {
        Long id = crearProducto(false);
        entrada(id, "10", "1000");
        salida(id, "3", TipoMovimiento.VENTA);
        salida(id, "1", TipoMovimiento.AJUSTE_SALIDA);

        mvc.perform(get("/api/v1/productos/" + id + "/movimientos").session(admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(3))
            .andExpect(jsonPath("$.contenido[0].saldoResultante").value("6.000"))
            .andExpect(jsonPath("$.contenido[1].saldoResultante").value("7.000"))
            .andExpect(jsonPath("$.contenido[2].saldoResultante").value("10.000"))
            .andExpect(jsonPath("$.contenido[2].costoUnitario").value("1000.0000"))
            .andExpect(jsonPath("$.contenido[0].usuario").value(nombreAdmin));
        mvc.perform(get("/api/v1/productos/" + id + "/movimientos").session(operador).param("tipo", "VENTA"))
            .andExpect(jsonPath("$.totalElementos").value(1))
            .andExpect(jsonPath("$.contenido[0].costoUnitario").doesNotExist());
        mvc.perform(get("/api/v1/inventario/existencias").session(operador).param("q", "").param("soloConExistencias", "true")
                .param("tamano", "100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contenido[?(@.productoId == %d)].existencias".formatted(id)).value("6.000"))
            .andExpect(jsonPath("$.contenido[0].costoPromedio").doesNotExist());
        mvc.perform(get("/api/v1/productos/999999999/movimientos").session(admin)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CA-INV-02: una salida sin existencias se rechaza y no deja rastro en el kardex")
    void salidaSinExistencias() {
        Long id = crearProducto(false);
        try {
            salida(id, "1", TipoMovimiento.VENTA);
        } catch (NegocioException e) {
            assertThat(e.getCodigo()).isEqualTo(CodigoError.EXISTENCIAS_INSUFICIENTES);
        }
        assertThat(existencias(id)).isEqualByComparingTo("0");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM movimiento_inventario WHERE producto_id = ?", Integer.class, id))
            .isZero();
    }

    /** Ejecuta las tareas a la vez (todas esperan la misma señal de salida) y devuelve cuántas tuvieron éxito. */
    private int simultaneas(int hilos, Callable<Boolean> tarea) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salidaSimultanea = new CountDownLatch(1);
        List<Future<Boolean>> resultados = new ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            resultados.add(pool.submit(() -> {
                salidaSimultanea.await();
                return tarea.call();
            }));
        }
        salidaSimultanea.countDown();
        int exitos = 0;
        for (Future<Boolean> r : resultados) {
            if (r.get()) {
                exitos++;
            }
        }
        pool.shutdown();
        return exitos;
    }

    @Test
    @DisplayName("RN-01 / CA-VEN-03: dos ventas simultáneas de la última unidad → solo una se confirma; saldo 0")
    void concurrenciaUltimaUnidad() throws Exception {
        Long id = crearProducto(false);
        entrada(id, "1", "500");
        int exitos = simultaneas(2, () -> {
            try {
                salida(id, "1", TipoMovimiento.VENTA);
                return true;
            } catch (NegocioException e) {
                return false;
            }
        });
        assertThat(exitos).isEqualTo(1);
        assertThat(existencias(id)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Estrés: 20 salidas simultáneas de 1 unidad sobre 10 existencias → exactamente 10 confirmadas")
    void estresSalidasSimultaneas() throws Exception {
        Long id = crearProducto(false);
        entrada(id, "10", "500");
        int exitos = simultaneas(20, () -> {
            try {
                salida(id, "1", TipoMovimiento.VENTA);
                return true;
            } catch (NegocioException e) {
                return false;
            }
        });
        assertThat(exitos).isEqualTo(10);
        assertThat(existencias(id)).isEqualByComparingTo("0");
        mvc.perform(get("/api/v1/inventario/verificacion").session(admin))
            .andExpect(jsonPath("$[?(@.productoId == %d)]".formatted(id)).isEmpty());
    }

    @Test
    @DisplayName("CA-DOC-02: 50 consecutivos simultáneos son distintos y seguidos; un fallo no consume número")
    void consecutivos() throws Exception {
        Set<String> numeros = java.util.Collections.synchronizedSet(new HashSet<>());
        simultaneas(50, () -> numeros.add(transaccion.execute(e -> consecutivos.siguiente(TipoDocumentoComercial.DEVOLUCION))));
        assertThat(numeros).hasSize(50).allMatch(n -> n.matches("DEV-\\d{6}"));
        List<Integer> valores = numeros.stream().map(n -> Integer.parseInt(n.substring(4))).sorted().toList();
        assertThat(valores.get(valores.size() - 1) - valores.get(0)).isEqualTo(49);

        String antes = transaccion.execute(e -> consecutivos.siguiente(TipoDocumentoComercial.DEVOLUCION));
        try {
            transaccion.executeWithoutResult(e -> {
                consecutivos.siguiente(TipoDocumentoComercial.DEVOLUCION);
                throw new IllegalStateException("fallo simulado del documento");
            });
        } catch (IllegalStateException esperado) {
            // la transacción del documento se revierte y el número no se consume
        }
        String despues = transaccion.execute(e -> consecutivos.siguiente(TipoDocumentoComercial.DEVOLUCION));
        assertThat(Integer.parseInt(despues.substring(4))).isEqualTo(Integer.parseInt(antes.substring(4)) + 1);
    }

    @Test
    @DisplayName("Valorización solo para ADMIN y verificación que detecta una alteración manual del saldo")
    void valorizacionYVerificacion() throws Exception {
        Long id = crearProducto(false);
        entrada(id, "4", "2500");
        mvc.perform(get("/api/v1/inventario/valorizacion").session(operador)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/inventario/valorizacion").session(admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").exists())
            .andExpect(jsonPath("$.categorias[0].valor").exists());

        jdbc.update("UPDATE producto SET existencias = 99 WHERE id = ?", id);
        mvc.perform(get("/api/v1/inventario/verificacion").session(admin))
            .andExpect(jsonPath("$[?(@.productoId == %d)].existenciasKardex".formatted(id)).value("4.000"));
        jdbc.update("UPDATE producto SET existencias = 4 WHERE id = ?", id);
        mvc.perform(get("/api/v1/inventario/verificacion").session(admin))
            .andExpect(jsonPath("$[?(@.productoId == %d)]".formatted(id)).isEmpty());
    }
}
