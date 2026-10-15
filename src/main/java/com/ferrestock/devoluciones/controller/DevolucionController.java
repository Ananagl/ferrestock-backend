/*
 * Nombre:                 DevolucionController.java
 * Descripción:            Endpoints de devoluciones de clientes (04_api_rest.md, 4.8).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.9.0
 */
package com.ferrestock.devoluciones.controller;

import java.time.LocalDate;

import com.ferrestock.compartido.respuesta.PaginaResponse;
import com.ferrestock.devoluciones.dto.respuesta.DevolucionResponse;
import com.ferrestock.devoluciones.dto.solicitud.DevolucionCrearRequest;
import com.ferrestock.devoluciones.servicio.DevolucionServicio;
import com.ferrestock.devoluciones.servicio.FiltroDevoluciones;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de devoluciones. Una devolución registrada no se modifica ni se anula.
 */
@RestController
@RequestMapping("/api/v1/devoluciones")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','OPERATIVO')")
@Tag(name = "Devoluciones")
public class DevolucionController {

    private final DevolucionServicio servicio;

    /**
     * @param clienteId cliente
     * @param ventaId   venta original
     * @param desde     fecha inicial (aaaa-mm-dd)
     * @param hasta     fecha final (aaaa-mm-dd)
     * @param pagina    número de página
     * @param tamano    tamaño (máximo 100)
     * @return devoluciones paginadas
     */
    @GetMapping
    @Operation(summary = "Lista devoluciones (RF-DEV-005)")
    public PaginaResponse<DevolucionResponse> listar(@RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long ventaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return servicio.listar(new FiltroDevoluciones(clienteId, ventaId, desde, hasta), pagina, tamano);
    }

    /**
     * @param id devolución
     * @return devolución con líneas
     */
    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una devolución con sus líneas y su validación")
    public DevolucionResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    /**
     * @param solicitud cliente, venta opcional, motivo y líneas
     * @return 201 con la devolución
     */
    @PostMapping
    @Operation(summary = "Registra y valida una devolución; las líneas aceptadas reingresan al inventario (RF-DEV-001 a 004)")
    @ApiResponse(responseCode = "400", description = "Motivo vacío, cantidad no válida o venta que no corresponde")
    @ApiResponse(responseCode = "409", description = "La cantidad devuelta acumulada supera la vendida")
    public ResponseEntity<DevolucionResponse> registrar(@Valid @RequestBody DevolucionCrearRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.registrar(solicitud));
    }
}
