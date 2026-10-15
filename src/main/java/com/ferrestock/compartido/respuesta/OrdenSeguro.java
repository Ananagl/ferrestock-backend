/*
 * Nombre:                 OrdenSeguro.java
 * Descripción:            Convierte el parámetro de ordenamiento de la API en un Sort validado contra una lista blanca de campos.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.3.0
 */
package com.ferrestock.compartido.respuesta;

import java.util.Locale;
import java.util.Set;

import com.ferrestock.compartido.error.CodigoError;
import com.ferrestock.compartido.error.NegocioException;
import org.springframework.data.domain.Sort;

/**
 * Ordenamiento seguro (05_seguridad.md, sección 4): solo se aceptan campos de una lista blanca, con el formato
 * {@code campo,asc} o {@code campo,desc}.
 */
public final class OrdenSeguro {

    private OrdenSeguro() {
    }

    /**
     * @param orden      valor recibido, por ejemplo {@code nombreUsuario,asc}; puede ser nulo
     * @param permitidos campos que se pueden ordenar
     * @param porDefecto orden si no se indica ninguno
     * @return ordenamiento validado
     * @throws NegocioException {@code VALIDACION} si el campo o la dirección no son válidos
     */
    public static Sort de(String orden, Set<String> permitidos, Sort porDefecto) {
        if (orden == null || orden.isBlank()) {
            return porDefecto;
        }
        String[] partes = orden.split(",");
        String campo = partes[0].trim();
        String direccion = partes.length > 1 ? partes[1].trim().toLowerCase(Locale.ROOT) : "asc";
        if (partes.length > 2 || !permitidos.contains(campo) || !Set.of("asc", "desc").contains(direccion)) {
            throw NegocioException.enCampo(CodigoError.VALIDACION, "orden");
        }
        return Sort.by("desc".equals(direccion) ? Sort.Direction.DESC : Sort.Direction.ASC, campo);
    }
}
