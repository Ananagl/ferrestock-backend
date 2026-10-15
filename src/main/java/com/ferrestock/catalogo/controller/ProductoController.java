/*
 * Nombre:                 ProductoController.java
 * Descripción:            Endpoints de productos y precios (04_api_rest.md, 4.3). ADMIN y OPERATIVO gestionan productos (DP-01); el costo es solo para ADMIN (DP-15).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.4.0
 */
package com.ferrestock.catalogo.controller;

import java.net.URI;

import com.ferrestock.catalogo.dto.respuesta.CostoProductoResponse;
import com.ferrestock.catalogo.dto.respuesta.ProductoResponse;
import com.ferrestock.catalogo.dto.solicitud.EstadoProductoRequest;
import com.ferrestock.catalogo.dto.solicitud.PrecioRequest;
import com.ferrestock.catalogo.dto.solicitud.ProductoActualizarRequest;
import com.ferrestock.catalogo.dto.solicitud.ProductoCrearRequest;
import com.ferrestock.catalogo.entidad.EstadoProducto;
import com.ferrestock.catalogo.servicio.FiltroProductos;
import com.ferrestock.catalogo.servicio.ProductoServicio;
import com.ferrestock.compartido.respuesta.PaginaResponse;
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
 * API de productos. No existe DELETE: los productos se desactivan (RN-02).
 */
@RestController
@RequestMapping("/api/v1/productos")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','OPERATIVO')")
@Tag(name = "Catálogo")
public class ProductoController {

    private final ProductoServicio servicio;

    /**
     * @param q           texto en código o nombre
     * @param categoriaId categoría
     * @param estado      ACTIVO o INACTIVO
     * @param pagina      número de página
     * @param tamano      tamaño (máximo 100)
     * @param orden       campo,dirección (codigo, nombre, precioVenta, existencias, creadoEn)
     * @return página de productos
     */
    @GetMapping
    @Operation(summary = "Lista productos con filtros, búsqueda y paginación (RF-PRO-004)")
    public PaginaResponse<ProductoResponse> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) EstadoProducto estado,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano,
            @RequestParam(required = false) String orden) {
        return servicio.listar(new FiltroProductos(q, categoriaId, estado), pagina, tamano, orden);
    }

    /**
     * @param id producto
     * @return producto
     */
    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un producto (RF-PRO-003)")
    @ApiResponse(responseCode = "404", description = "No existe")
    public ProductoResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    /**
     * @param solicitud datos del producto
     * @return 201 con el producto
     */
    @PostMapping
    @Operation(summary = "Registra un producto (RF-PRO-001)")
    @ApiResponse(responseCode = "409", description = "Código duplicado o categoría inactiva")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoCrearRequest solicitud) {
        ProductoResponse creado = servicio.crear(solicitud);
        return ResponseEntity.created(URI.create("/api/v1/productos/" + creado.id())).body(creado);
    }

    /**
     * @param id        producto
     * @param solicitud datos descriptivos y versión
     * @return producto actualizado
     */
    @PutMapping("/{id}")
    @Operation(summary = "Modifica los datos descriptivos (RF-PRO-005)")
    @ApiResponse(responseCode = "409", description = "Versión desactualizada o categoría inactiva")
    public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProductoActualizarRequest solicitud) {
        return servicio.actualizar(id, solicitud);
    }

    /**
     * @param id        producto
     * @param solicitud nuevo estado
     * @return producto actualizado
     */
    @PatchMapping("/{id}/estado")
    @Operation(summary = "Activa o desactiva un producto (RF-PRO-006, RF-PRO-007)")
    public ProductoResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoProductoRequest solicitud) {
        return servicio.cambiarEstado(id, solicitud.estado());
    }

    /**
     * @param id        producto
     * @param solicitud nuevo precio
     * @return producto actualizado
     */
    @PatchMapping("/{id}/precio")
    @Operation(summary = "Cambia el precio de venta y lo audita (RF-PRE-001, RF-PRE-002)")
    public ProductoResponse cambiarPrecio(@PathVariable Long id, @Valid @RequestBody PrecioRequest solicitud) {
        return servicio.cambiarPrecio(id, solicitud.precioVenta());
    }

    /**
     * @param id producto
     * @return costo promedio y valor del inventario
     */
    @GetMapping("/{id}/costo")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Costo promedio y valor del inventario (solo ADMIN, RF-COS-002)")
    public CostoProductoResponse costo(@PathVariable Long id) {
        return servicio.costo(id);
    }
}
