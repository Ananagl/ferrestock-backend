/*
 * Nombre:                 AjusteController.java
 * Descripción:            Endpoints de ajustes de inventario, solo para el Administrador (04_api_rest.md, 4.9).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.7.0
 */
package com.ferrestock.ajustes.controller;

import java.time.LocalDate;

import com.ferrestock.ajustes.dto.respuesta.AjusteResponse;
import com.ferrestock.ajustes.dto.solicitud.AjusteCrearRequest;
import com.ferrestock.ajustes.entidad.TipoAjuste;
import com.ferrestock.ajustes.servicio.AjusteServicio;
import com.ferrestock.ajustes.servicio.FiltroAjustes;
import com.ferrestock.compartido.respuesta.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de ajustes. Todo el controlador exige el rol ADMIN (RN-08, DP-16): un operativo recibe 403 y el intento queda
 * auditado. No hay PUT ni DELETE: un ajuste equivocado se corrige con otro ajuste en sentido contrario.
 */
@RestController
@RequestMapping("/api/v1/ajustes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Ajustes")
public class AjusteController {

    private final AjusteServicio servicio;

    /**
     * @param productoId producto
     * @param desde      fecha inicial (aaaa-mm-dd)
     * @param hasta      fecha final (aaaa-mm-dd)
     * @param tipo       ENTRADA o SALIDA
     * @param pagina     número de página
     * @param tamano     tamaño (máximo 100)
     * @return historial paginado
     */
    @GetMapping
    @Operation(summary = "Historial de ajustes con motivo y responsable (RF-AJU-004)")
    public PaginaResponse<AjusteResponse> historial(@RequestParam(required = false) Long productoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) TipoAjuste tipo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return servicio.historial(new FiltroAjustes(productoId, desde, hasta, tipo), pagina, tamano);
    }

    /**
     * @param solicitud datos del ajuste
     * @return 201 con el ajuste y las existencias resultantes
     */
    @PostMapping
    @Operation(summary = "Registra un ajuste de entrada o salida, incluido el inventario inicial (RF-AJU-001 a 003)")
    @ApiResponse(responseCode = "400", description = "Motivo vacío, cantidad no válida o entrada sin costo")
    @ApiResponse(responseCode = "403", description = "El usuario no es ADMIN")
    @ApiResponse(responseCode = "409", description = "Producto inactivo o existencias insuficientes")
    public ResponseEntity<AjusteResponse> registrar(@Valid @RequestBody AjusteCrearRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.registrar(solicitud));
    }
}
