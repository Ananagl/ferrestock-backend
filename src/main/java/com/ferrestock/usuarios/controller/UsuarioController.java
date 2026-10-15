/*
 * Nombre:                 UsuarioController.java
 * Descripción:            Endpoints de gestión de usuarios, exclusivos del Administrador (04_api_rest.md, sección 4.2).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.3.0
 */
package com.ferrestock.usuarios.controller;

import java.net.URI;

import com.ferrestock.compartido.respuesta.PaginaResponse;
import com.ferrestock.usuarios.dto.respuesta.UsuarioResponse;
import com.ferrestock.usuarios.dto.solicitud.EstadoUsuarioRequest;
import com.ferrestock.usuarios.dto.solicitud.RestablecimientoRequest;
import com.ferrestock.usuarios.dto.solicitud.UsuarioActualizarRequest;
import com.ferrestock.usuarios.dto.solicitud.UsuarioCrearRequest;
import com.ferrestock.usuarios.entidad.Rol;
import com.ferrestock.usuarios.servicio.FiltroUsuarios;
import com.ferrestock.usuarios.servicio.UsuarioServicio;
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
 * API de usuarios. Todos los métodos exigen el rol ADMIN; no existe DELETE (los usuarios se desactivan).
 */
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Usuarios")
public class UsuarioController {

    private final UsuarioServicio servicio;

    /**
     * @param q      texto en nombre de usuario o nombre completo
     * @param rol    rol
     * @param activo estado
     * @param pagina número de página
     * @param tamano tamaño (máximo 100)
     * @param orden  campo,dirección (nombreUsuario, nombreCompleto, rol, creadoEn)
     * @return página de usuarios
     */
    @GetMapping
    @Operation(summary = "Lista usuarios con filtros y paginación")
    public PaginaResponse<UsuarioResponse> listar(@RequestParam(required = false) String q,
                                                  @RequestParam(required = false) Rol rol,
                                                  @RequestParam(required = false) Boolean activo,
                                                  @RequestParam(defaultValue = "0") int pagina,
                                                  @RequestParam(defaultValue = "20") int tamano,
                                                  @RequestParam(required = false) String orden) {
        return servicio.listar(new FiltroUsuarios(q, rol, activo), pagina, tamano, orden);
    }

    /**
     * @param id identificador
     * @return usuario
     */
    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un usuario")
    @ApiResponse(responseCode = "404", description = "No existe")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    /**
     * @param solicitud datos del usuario
     * @return 201 con el usuario y la cabecera Location
     */
    @PostMapping
    @Operation(summary = "Crea un usuario con contraseña temporal")
    @ApiResponse(responseCode = "201", description = "Creado")
    @ApiResponse(responseCode = "409", description = "Nombre de usuario duplicado")
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioCrearRequest solicitud) {
        UsuarioResponse creado = servicio.crear(solicitud);
        return ResponseEntity.created(URI.create("/api/v1/usuarios/" + creado.id())).body(creado);
    }

    /**
     * @param id        identificador
     * @param solicitud nombre completo y rol
     * @return usuario actualizado
     */
    @PutMapping("/{id}")
    @Operation(summary = "Modifica nombre completo y rol")
    @ApiResponse(responseCode = "409", description = "Un administrador no puede cambiar su propio rol")
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioActualizarRequest solicitud) {
        return servicio.actualizar(id, solicitud);
    }

    /**
     * @param id        identificador
     * @param solicitud nuevo estado
     * @return usuario actualizado
     */
    @PatchMapping("/{id}/estado")
    @Operation(summary = "Activa o desactiva un usuario")
    @ApiResponse(responseCode = "409", description = "Un administrador no puede desactivarse a sí mismo")
    public UsuarioResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoUsuarioRequest solicitud) {
        return servicio.cambiarEstado(id, solicitud.activo());
    }

    /**
     * @param id        identificador
     * @param solicitud contraseña temporal
     * @return 204 sin contenido
     */
    @PostMapping("/{id}/restablecimiento")
    @Operation(summary = "Asigna una contraseña temporal y desbloquea la cuenta")
    public ResponseEntity<Void> restablecer(@PathVariable Long id, @Valid @RequestBody RestablecimientoRequest solicitud) {
        servicio.restablecerContrasena(id, solicitud.contrasenaTemporal());
        return ResponseEntity.noContent().build();
    }
}
