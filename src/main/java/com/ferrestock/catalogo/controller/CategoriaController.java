/*
 * Nombre:                 CategoriaController.java
 * Descripción:            Endpoints de categorías: consulta para todos los roles y gestión solo para ADMIN (04_api_rest.md, 4.3).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.4.0
 */
package com.ferrestock.catalogo.controller;

import java.net.URI;
import java.util.List;

import com.ferrestock.catalogo.dto.respuesta.CategoriaResponse;
import com.ferrestock.catalogo.dto.solicitud.CategoriaRequest;
import com.ferrestock.catalogo.dto.solicitud.EstadoCategoriaRequest;
import com.ferrestock.catalogo.servicio.CategoriaServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de categorías (RF-PRO-009). El listado no se pagina porque es corto.
 */
@RestController
@RequestMapping("/api/v1/categorias")
@RequiredArgsConstructor
@Tag(name = "Catálogo")
public class CategoriaController {

    private final CategoriaServicio servicio;

    /**
     * @param activa filtro opcional de estado
     * @return categorías ordenadas por nombre
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIVO')")
    @Operation(summary = "Lista las categorías")
    public List<CategoriaResponse> listar(@RequestParam(required = false) Boolean activa) {
        return servicio.listar(activa);
    }

    /**
     * @param solicitud nombre
     * @return 201 con la categoría
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crea una categoría (ADMIN)")
    @ApiResponse(responseCode = "409", description = "Nombre duplicado")
    public ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CategoriaRequest solicitud) {
        CategoriaResponse creada = servicio.crear(solicitud.nombre());
        return ResponseEntity.created(URI.create("/api/v1/categorias/" + creada.id())).body(creada);
    }

    /**
     * @param id        categoría
     * @param solicitud nuevo nombre
     * @return categoría actualizada
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Renombra una categoría (ADMIN)")
    public CategoriaResponse renombrar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest solicitud) {
        return servicio.renombrar(id, solicitud.nombre());
    }

    /**
     * @param id        categoría
     * @param solicitud nuevo estado
     * @return categoría actualizada
     */
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activa o desactiva una categoría (ADMIN)")
    public CategoriaResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoCategoriaRequest solicitud) {
        return servicio.cambiarEstado(id, solicitud.activa());
    }
}
