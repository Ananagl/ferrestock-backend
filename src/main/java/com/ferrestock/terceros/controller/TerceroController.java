/*
 * Nombre:                 TerceroController.java
 * Descripción:            Endpoints de clientes y proveedores (04_api_rest.md, 4.4). Desactivar es solo del ADMIN (DP-06).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.5.0
 */
package com.ferrestock.terceros.controller;

import java.net.URI;

import com.ferrestock.compartido.respuesta.PaginaResponse;
import com.ferrestock.terceros.dto.respuesta.TerceroResponse;
import com.ferrestock.terceros.dto.solicitud.EstadoTerceroRequest;
import com.ferrestock.terceros.dto.solicitud.TerceroActualizarRequest;
import com.ferrestock.terceros.dto.solicitud.TerceroCrearRequest;
import com.ferrestock.terceros.entidad.TipoPersona;
import com.ferrestock.terceros.servicio.FiltroTerceros;
import com.ferrestock.terceros.servicio.TerceroServicio;
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
 * API de terceros. No existe DELETE: los terceros se desactivan.
 */
@RestController
@RequestMapping("/api/v1/terceros")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','OPERATIVO')")
@Tag(name = "Terceros")
public class TerceroController {

    private final TerceroServicio servicio;

    /**
     * @param q           texto en nombre o número de documento
     * @param rol         CLIENTE o PROVEEDOR
     * @param tipoPersona NATURAL o JURIDICA
     * @param activo      estado
     * @param pagina      número de página
     * @param tamano      tamaño (máximo 100)
     * @param orden       campo,dirección (nombre, numeroDocumento, creadoEn)
     * @return página de terceros
     */
    @GetMapping
    @Operation(summary = "Lista clientes y proveedores (RF-TER-004)")
    public PaginaResponse<TerceroResponse> listar(@RequestParam(required = false) String q,
                                                  @RequestParam(required = false) FiltroTerceros.RolTercero rol,
                                                  @RequestParam(required = false) TipoPersona tipoPersona,
                                                  @RequestParam(required = false) Boolean activo,
                                                  @RequestParam(defaultValue = "0") int pagina,
                                                  @RequestParam(defaultValue = "20") int tamano,
                                                  @RequestParam(required = false) String orden) {
        return servicio.listar(new FiltroTerceros(q, rol, tipoPersona, activo), pagina, tamano, orden);
    }

    /**
     * @param id tercero
     * @return tercero
     */
    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un tercero")
    public TerceroResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    /**
     * @param solicitud datos del tercero
     * @return 201 con el tercero
     */
    @PostMapping
    @Operation(summary = "Registra un cliente o proveedor (RF-TER-001 a 003)")
    @ApiResponse(responseCode = "400", description = "Datos inválidos, DV del NIT incorrecto o sin rol")
    @ApiResponse(responseCode = "409", description = "Documento duplicado")
    public ResponseEntity<TerceroResponse> crear(@Valid @RequestBody TerceroCrearRequest solicitud) {
        TerceroResponse creado = servicio.crear(solicitud);
        return ResponseEntity.created(URI.create("/api/v1/terceros/" + creado.id())).body(creado);
    }

    /**
     * @param id        tercero
     * @param solicitud nombre, contacto y roles
     * @return tercero actualizado
     */
    @PutMapping("/{id}")
    @Operation(summary = "Modifica nombre, contacto y roles (RF-TER-005)")
    public TerceroResponse actualizar(@PathVariable Long id, @Valid @RequestBody TerceroActualizarRequest solicitud) {
        return servicio.actualizar(id, solicitud);
    }

    /**
     * @param id        tercero
     * @param solicitud nuevo estado
     * @return tercero actualizado
     */
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activa o desactiva un tercero (solo ADMIN, RF-TER-006)")
    public TerceroResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoTerceroRequest solicitud) {
        return servicio.cambiarEstado(id, solicitud.activo());
    }
}
