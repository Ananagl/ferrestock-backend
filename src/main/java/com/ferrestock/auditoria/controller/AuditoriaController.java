/*
 * Nombre:                 AuditoriaController.java
 * Descripción:            Endpoint de consulta de auditoría, exclusivo del Administrador (RF-AUD-002).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.2.0
 */
package com.ferrestock.auditoria.controller;

import java.time.LocalDate;

import com.ferrestock.auditoria.dto.respuesta.AuditoriaResponse;
import com.ferrestock.auditoria.entidad.AccionAuditoria;
import com.ferrestock.auditoria.servicio.AuditoriaServicio;
import com.ferrestock.auditoria.servicio.FiltroAuditoria;
import com.ferrestock.compartido.respuesta.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta de eventos de auditoría. No existen operaciones para crear, modificar ni borrar eventos
 * (RF-AUD-003).
 */
@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
@Tag(name = "Auditoría")
public class AuditoriaController {

    private final AuditoriaServicio servicio;

    /**
     * Lista eventos paginados con filtros opcionales.
     *
     * @param usuarioId usuario responsable
     * @param accion    código de acción
     * @param desde     fecha inicial (aaaa-mm-dd)
     * @param hasta     fecha final (aaaa-mm-dd)
     * @param q         texto a buscar
     * @param pagina    número de página
     * @param tamano    tamaño de página (máximo 100)
     * @return página de eventos
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Consulta la auditoría (solo ADMIN)")
    @ApiResponse(responseCode = "200", description = "Página de eventos")
    @ApiResponse(responseCode = "403", description = "El rol no es ADMIN")
    public PaginaResponse<AuditoriaResponse> consultar(
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) AccionAuditoria accion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return servicio.consultar(new FiltroAuditoria(usuarioId, accion, desde, hasta, q), pagina, tamano);
    }
}
