/*
 * Nombre:                 VentaController.java
 * Descripción:            Endpoints de ventas (04_api_rest.md, 4.7). Anular es solo del ADMIN (DP-07).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.9.0
 */
package com.ferrestock.ventas.controller;

import java.time.LocalDate;
import java.util.List;

import com.ferrestock.compartido.documentos.AnulacionRequest;
import com.ferrestock.compartido.documentos.EstadoDocumento;
import com.ferrestock.compartido.respuesta.PaginaResponse;
import com.ferrestock.ventas.dto.respuesta.DevolvibleResponse;
import com.ferrestock.ventas.dto.respuesta.VentaResponse;
import com.ferrestock.ventas.dto.solicitud.VentaCrearRequest;
import com.ferrestock.ventas.servicio.FiltroVentas;
import com.ferrestock.ventas.servicio.VentaServicio;
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
 * API de ventas. No hay PUT ni DELETE: una venta equivocada se anula (RNP-05).
 */
@RestController
@RequestMapping("/api/v1/ventas")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','OPERATIVO')")
@Tag(name = "Ventas")
public class VentaController {

    private final VentaServicio servicio;

    /**
     * @param clienteId cliente
     * @param desde     fecha inicial (aaaa-mm-dd)
     * @param hasta     fecha final (aaaa-mm-dd)
     * @param estado    REGISTRADA o ANULADA
     * @param pagina    número de página
     * @param tamano    tamaño (máximo 100)
     * @return ventas paginadas
     */
    @GetMapping
    @Operation(summary = "Lista ventas (RF-VEN-005)")
    public PaginaResponse<VentaResponse> listar(@RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) EstadoDocumento estado,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return servicio.listar(new FiltroVentas(clienteId, desde, hasta, estado), pagina, tamano);
    }

    /**
     * @param id venta
     * @return venta con líneas y pago
     */
    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una venta con líneas, pago y consecutivo")
    public VentaResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    /**
     * @param id venta
     * @return por cada línea, cuánto queda por devolver
     */
    @GetMapping("/{id}/devolvible")
    @Operation(summary = "Cantidades que aún se pueden devolver de una venta (RF-DEV-004)")
    public List<DevolvibleResponse> devolvible(@PathVariable Long id) {
        return servicio.devolvible(id);
    }

    /**
     * @param solicitud cliente, líneas y pago
     * @return 201 con la venta confirmada
     */
    @PostMapping
    @Operation(summary = "Confirma una venta con pago en efectivo (RF-VEN-001 a 004, RF-PAG-001 a 003)")
    @ApiResponse(responseCode = "400", description = "Línea inválida, pago insuficiente u otro medio de pago")
    @ApiResponse(responseCode = "409", description = "Existencias insuficientes, cliente o producto inactivo")
    public ResponseEntity<VentaResponse> registrar(@Valid @RequestBody VentaCrearRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.registrar(solicitud));
    }

    /**
     * @param id        venta
     * @param solicitud motivo
     * @return venta anulada
     */
    @PostMapping("/{id}/anulacion")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Anula una venta; las cantidades reingresan al costo histórico (solo ADMIN, RF-VEN-006)")
    @ApiResponse(responseCode = "409", description = "Ya anulada o con devoluciones")
    public VentaResponse anular(@PathVariable Long id, @Valid @RequestBody AnulacionRequest solicitud) {
        return servicio.anular(id, solicitud.motivo());
    }
}
