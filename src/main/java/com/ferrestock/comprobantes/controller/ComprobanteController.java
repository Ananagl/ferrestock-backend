/*
 * Nombre:                 ComprobanteController.java
 * Descripción:            Endpoints de comprobantes (04_api_rest.md, 4.10).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.10.0
 */
package com.ferrestock.comprobantes.controller;

import java.time.LocalDate;
import java.util.Locale;

import com.ferrestock.compartido.documentos.TipoDocumentoComercial;
import com.ferrestock.compartido.respuesta.PaginaResponse;
import com.ferrestock.comprobantes.dto.respuesta.ComprobanteResponse;
import com.ferrestock.comprobantes.dto.respuesta.ComprobanteResumen;
import com.ferrestock.comprobantes.servicio.ComprobanteServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de comprobantes. Solo lectura: los comprobantes se arman desde los documentos.
 */
@RestController
@RequestMapping("/api/v1/comprobantes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','OPERATIVO')")
@Tag(name = "Comprobantes")
public class ComprobanteController {

    private final ComprobanteServicio servicio;

    /**
     * @param tipo        COMPRA, VENTA, DEVOLUCION o AJUSTE
     * @param consecutivo texto del consecutivo
     * @param desde       fecha inicial (aaaa-mm-dd)
     * @param hasta       fecha final (aaaa-mm-dd)
     * @param pagina      número de página
     * @param tamano      tamaño (máximo 100)
     * @return comprobantes (sin ajustes para el operativo)
     */
    @GetMapping
    @Operation(summary = "Búsqueda unificada de comprobantes (RF-DOC-005)")
    public PaginaResponse<ComprobanteResumen> buscar(@RequestParam(required = false) TipoDocumentoComercial tipo,
            @RequestParam(required = false) String consecutivo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return servicio.buscar(tipo, consecutivo, desde, hasta, pagina, tamano);
    }

    /**
     * @param tipo        tipo de documento
     * @param consecutivo consecutivo exacto
     * @param idioma      idioma de la leyenda
     * @return comprobante imprimible
     */
    @GetMapping("/{tipo}/{consecutivo}")
    @Operation(summary = "Comprobante de un documento con la leyenda de simulación (RF-DOC-002 a 004)")
    public ComprobanteResponse obtener(@PathVariable TipoDocumentoComercial tipo, @PathVariable String consecutivo, Locale idioma) {
        return servicio.obtener(tipo, consecutivo, idioma);
    }
}
