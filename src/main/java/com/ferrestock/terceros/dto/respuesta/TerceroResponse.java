/*
 * Nombre:                 TerceroResponse.java
 * Descripción:            DTO de salida de un tercero.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.5.0
 */
package com.ferrestock.terceros.dto.respuesta;

import java.time.Instant;

import com.ferrestock.terceros.entidad.Tercero;

/**
 * @param id                 identificador
 * @param tipoPersona        NATURAL o JURIDICA
 * @param tipoDocumento      CC, CE o NIT
 * @param numeroDocumento    número
 * @param digitoVerificacion DV del NIT, o nulo
 * @param nombre             nombre o razón social
 * @param telefono           teléfono
 * @param correo             correo
 * @param direccion          dirección
 * @param esCliente          rol de cliente
 * @param esProveedor        rol de proveedor
 * @param activo             estado
 * @param creadoEn           fecha de alta
 * @param actualizadoEn      fecha de la última modificación
 */
public record TerceroResponse(Long id, String tipoPersona, String tipoDocumento, String numeroDocumento,
                              String digitoVerificacion, String nombre, String telefono, String correo,
                              String direccion, boolean esCliente, boolean esProveedor, boolean activo,
                              Instant creadoEn, Instant actualizadoEn) {

    /**
     * @param tercero entidad
     * @return DTO equivalente
     */
    public static TerceroResponse desde(Tercero tercero) {
        return new TerceroResponse(tercero.getId(), tercero.getTipoPersona().name(), tercero.getTipoDocumento().name(),
            tercero.getNumeroDocumento(), tercero.getDigitoVerificacion(), tercero.getNombre(), tercero.getTelefono(),
            tercero.getCorreo(), tercero.getDireccion(), tercero.isEsCliente(), tercero.isEsProveedor(),
            tercero.isActivo(), tercero.getCreadoEn(), tercero.getActualizadoEn());
    }
}
