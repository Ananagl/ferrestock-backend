/*
 * Nombre:                 CompraController.java
 * Descripción:            Endpoints de compras (04_api_rest.md, 4.6). Anular es solo del ADMIN (DP-07).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.8.0
 */
package com.ferrestock.compras.controller;

import java.time.LocalDate;

import com.ferrestock.compartido.documentos.AnulacionRequest;
import com.ferrestock.compartido.documentos.EstadoDocumento;
import com.ferrestock.compartido.respuesta.PaginaResponse;
import com.ferrestock.compras.dto.respuesta.CompraResponse;
import com.ferrestock.compras.dto.solicitud.CompraCrearRequest;
import com.ferrestock.compras.servicio.CompraServicio;
import com.ferrestock.compras.servicio.FiltroCompras;
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
 * API de compras. No hay PUT ni DELETE: una compra equivocada se anula (RNP-05).
 */
@RestController
@RequestMapping("/api/v1/compras")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','OPERATIVO')")
@Tag(name = "Compras")
public class CompraController {

    private final CompraServicio servicio;

    /**
     * @param proveedorId proveedor
     * @param desde       fecha inicial (aaaa-mm-dd)
     * @param hasta       fecha final (aaaa-mm-dd)
     * @param estado      REGISTRADA o ANULADA
     * @param pagina      número de página
     * @param tamano      tamaño (máximo 100)
     * @return compras paginadas
     */
    @GetMapping
    @Operation(summary = "Lista compras (RF-COM-004)")
    public PaginaResponse<CompraResponse> listar(@RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) EstadoDocumento estado,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return servicio.listar(new FiltroCompras(proveedorId, desde, hasta, estado), pagina, tamano);
    }

    /**
     * @param id compra
     * @return compra con líneas
     */
    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una compra con sus líneas")
    public CompraResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    /**
     * @param solicitud proveedor y líneas
     * @return 201 con la compra
     */
    @PostMapping
    @Operation(summary = "Registra y recibe una compra completa (RF-COM-001 a 003)")
    @ApiResponse(responseCode = "400", description = "Línea inválida (se indica cuál) o producto repetido")
    @ApiResponse(responseCode = "409", description = "Proveedor o producto inactivo")
    public ResponseEntity<CompraResponse> registrar(@Valid @RequestBody CompraCrearRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.registrar(solicitud));
    }

    /**
     * @param id        compra
     * @param solicitud motivo
     * @return compra anulada
     */
    @PostMapping("/{id}/anulacion")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Anula una compra con movimientos compensatorios (solo ADMIN, RF-COM-005)")
    @ApiResponse(responseCode = "409", description = "Ya anulada, existencias insuficientes o entradas posteriores")
    public CompraResponse anular(@PathVariable Long id, @Valid @RequestBody AnulacionRequest solicitud) {
        return servicio.anular(id, solicitud.motivo());
    }
}
