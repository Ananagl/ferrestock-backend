/*
 * Nombre:                 InventarioController.java
 * Descripción:            Endpoints de inventario: existencias, kardex, valorización y verificación (04_api_rest.md, 4.5).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.6.0
 */
package com.ferrestock.inventario.controller;

import java.time.LocalDate;
import java.util.List;

import com.ferrestock.catalogo.servicio.FiltroProductos;
import com.ferrestock.compartido.respuesta.PaginaResponse;
import com.ferrestock.inventario.dto.respuesta.ExistenciaResponse;
import com.ferrestock.inventario.dto.respuesta.InconsistenciaResponse;
import com.ferrestock.inventario.dto.respuesta.MovimientoResponse;
import com.ferrestock.inventario.dto.respuesta.ValorizacionResponse;
import com.ferrestock.inventario.entidad.TipoMovimiento;
import com.ferrestock.inventario.servicio.FiltroKardex;
import com.ferrestock.inventario.servicio.InventarioConsultaServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de consulta de inventario. Los movimientos los crean los módulos de compras, ventas, devoluciones y ajustes;
 * aquí solo se consultan.
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','OPERATIVO')")
@Tag(name = "Inventario")
public class InventarioController {

    private final InventarioConsultaServicio servicio;

    /**
     * @param q                  texto en código o nombre
     * @param categoriaId        categoría
     * @param soloConExistencias si solo se listan productos con existencias
     * @param pagina             número de página
     * @param tamano             tamaño (máximo 100)
     * @return existencias paginadas
     */
    @GetMapping("/api/v1/inventario/existencias")
    @Operation(summary = "Existencias por producto (RF-INV-001); el costo solo se incluye para ADMIN")
    public PaginaResponse<ExistenciaResponse> existencias(@RequestParam(required = false) String q,
                                                          @RequestParam(required = false) Long categoriaId,
                                                          @RequestParam(defaultValue = "false") boolean soloConExistencias,
                                                          @RequestParam(defaultValue = "0") int pagina,
                                                          @RequestParam(defaultValue = "20") int tamano) {
        return servicio.existencias(new FiltroProductos(q, categoriaId, null), soloConExistencias, pagina, tamano);
    }

    /**
     * @param id     producto
     * @param desde  fecha inicial (aaaa-mm-dd)
     * @param hasta  fecha final (aaaa-mm-dd)
     * @param tipo   tipo de movimiento
     * @param pagina número de página
     * @param tamano tamaño (máximo 100)
     * @return kardex paginado
     */
    @GetMapping("/api/v1/productos/{id}/movimientos")
    @Operation(summary = "Kardex de un producto (RF-INV-003)")
    public PaginaResponse<MovimientoResponse> kardex(@PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) TipoMovimiento tipo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return servicio.kardex(id, new FiltroKardex(desde, hasta, tipo), pagina, tamano);
    }

    /**
     * @return valor del inventario total y por categoría
     */
    @GetMapping("/api/v1/inventario/valorizacion")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Valor del inventario total y por categoría (solo ADMIN, RF-COS-002)")
    public ValorizacionResponse valorizacion() {
        return servicio.valorizacion();
    }

    /**
     * @return productos cuyas existencias o costo no coinciden con el kardex (vacío si todo es consistente)
     */
    @GetMapping("/api/v1/inventario/verificacion")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Verifica la consistencia entre productos y kardex (solo ADMIN, RNF-FIA-01)")
    public List<InconsistenciaResponse> verificar() {
        return servicio.verificar();
    }
}
