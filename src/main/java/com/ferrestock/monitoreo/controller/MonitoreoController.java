/*
 * Nombre:                 MonitoreoController.java
 * Descripción:            Endpoints de monitoreo para el ADMIN: errores registrados y métricas de operación (04_api_rest.md, 4.11).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.13.0
 */
package com.ferrestock.monitoreo.controller;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import com.ferrestock.compartido.documentos.LineasDocumento;
import com.ferrestock.compartido.respuesta.PaginaResponse;
import com.ferrestock.monitoreo.dto.MetricasResponse;
import com.ferrestock.monitoreo.entidad.RegistroError;
import com.ferrestock.monitoreo.repositorio.RegistroErrorRepositorio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de monitoreo (solo ADMIN, DP-16). Las métricas del equipo salen de Taiga (scripts/metricas, Fase 7).
 */
@RestController
@RequestMapping("/api/v1/monitoreo")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Monitoreo")
public class MonitoreoController {

    private final RegistroErrorRepositorio errores;
    private final JdbcTemplate jdbc;
    private final Clock reloj;

    /**
     * @param desde  fecha inicial (aaaa-mm-dd; por defecto, hace 7 días)
     * @param hasta  fecha final (aaaa-mm-dd; por defecto, hoy)
     * @param pagina número de página
     * @param tamano tamaño (máximo 100)
     * @return errores del período con su correlación y referencia de Taiga
     */
    @GetMapping("/errores")
    @Transactional(readOnly = true)
    @Operation(summary = "Errores registrados con su referencia en Taiga (RF-MON-006)")
    public PaginaResponse<Map<String, Object>> errores(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamano) {
        LocalDate hoy = LocalDate.now(reloj.withZone(LineasDocumento.ZONA_NEGOCIO));
        Instant inicio = (desde == null ? hoy.minusDays(7) : desde).atStartOfDay(LineasDocumento.ZONA_NEGOCIO).toInstant();
        Instant fin = (hasta == null ? hoy : hasta).plusDays(1).atStartOfDay(LineasDocumento.ZONA_NEGOCIO).toInstant();
        return PaginaResponse.de(errores.findByFechaBetweenOrderByFechaDesc(inicio, fin,
            PaginaResponse.solicitud(pagina, tamano, Sort.unsorted())).map(MonitoreoController::vista));
    }

    /**
     * @return métricas de operación del día y de las últimas 24 horas
     */
    @GetMapping("/metricas")
    @Transactional(readOnly = true)
    @Operation(summary = "Métricas de operación del sistema (RF-MON-005)")
    public MetricasResponse metricas() {
        Instant inicioDia = LocalDate.now(reloj.withZone(LineasDocumento.ZONA_NEGOCIO)).atStartOfDay(LineasDocumento.ZONA_NEGOCIO).toInstant();
        Instant hace24h = reloj.instant().minus(24, ChronoUnit.HOURS);
        java.sql.Timestamp dia = java.sql.Timestamp.from(inicioDia);
        java.sql.Timestamp ayer = java.sql.Timestamp.from(hace24h);
        return new MetricasResponse(
            contar("SELECT count(*) FROM venta WHERE fecha >= ?", dia),
            jdbc.queryForObject("SELECT COALESCE(sum(total), 0) FROM venta WHERE fecha >= ? AND estado = 'REGISTRADA'", BigDecimal.class, dia),
            contar("SELECT count(*) FROM compra WHERE fecha >= ?", dia),
            contar("SELECT count(*) FROM devolucion WHERE fecha >= ?", dia),
            contar("SELECT (SELECT count(*) FROM venta WHERE anulada_en >= ?) + (SELECT count(*) FROM compra WHERE anulada_en >= ?)", dia, dia),
            contar("SELECT count(*) FROM producto WHERE estado = 'ACTIVO' AND existencias = 0"),
            jdbc.queryForObject("SELECT COALESCE(round(sum(existencias * costo_promedio), 2), 0) FROM producto", BigDecimal.class),
            errores.countByFechaAfter(hace24h),
            contar("SELECT count(*) FROM registro_error WHERE fecha >= ? AND taiga_ref IS NOT NULL", ayer),
            contar("SELECT count(*) FROM auditoria WHERE accion = 'ACCESO_DENEGADO' AND fecha >= ?", ayer),
            contar("SELECT count(*) FROM auditoria WHERE accion = 'LOGIN_FALLIDO' AND fecha >= ?", ayer));
    }

    private long contar(String sql, Object... parametros) {
        Long valor = jdbc.queryForObject(sql, Long.class, parametros);
        return valor == null ? 0 : valor;
    }

    private static Map<String, Object> vista(RegistroError e) {
        return Map.of("correlacion", e.getCorrelacion().toString(), "fecha", e.getFecha(), "metodo", String.valueOf(e.getMetodo()),
            "ruta", String.valueOf(e.getRuta()), "tipo", e.getTipo(), "taigaRef", e.getTaigaRef() == null ? "" : e.getTaigaRef());
    }
}
