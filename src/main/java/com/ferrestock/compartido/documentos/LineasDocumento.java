/*
 * Nombre:                 LineasDocumento.java
 * Descripción:            Reglas comunes a las líneas de compras, ventas y devoluciones.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.8.0
 */
package com.ferrestock.compartido.documentos;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ferrestock.compartido.error.CodigoError;
import com.ferrestock.compartido.error.NegocioException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

/**
 * Utilidades compartidas por los documentos con líneas.
 */
public final class LineasDocumento {

    /** Zona horaria del negocio para los filtros por fecha. */
    public static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Bogota");

    private LineasDocumento() {
    }

    /**
     * @param indice posición de la línea
     * @param campo  campo de la línea
     * @return nombre del campo tal como lo reporta la validación, por ejemplo {@code lineas[1].cantidad}
     */
    public static String campo(int indice, String campo) {
        return "lineas[" + indice + "]." + campo;
    }

    /**
     * Rechaza un producto repetido en dos líneas (DP-19), señalando la segunda.
     *
     * @param productoIds productos en el orden de las líneas
     * @throws NegocioException {@code PRODUCTO_REPETIDO}
     */
    public static void exigirSinRepetidos(List<Long> productoIds) {
        Set<Long> vistos = new HashSet<>();
        for (int i = 0; i < productoIds.size(); i++) {
            if (!vistos.add(productoIds.get(i))) {
                throw NegocioException.enCampo(CodigoError.PRODUCTO_REPETIDO, campo(i, "productoId"));
            }
        }
    }

    /**
     * Agrega las condiciones de un rango de fechas inclusivo en la zona del negocio.
     *
     * @param condiciones lista a completar
     * @param cb          constructor de criterios
     * @param fecha       columna de fecha
     * @param desde       fecha inicial o nula
     * @param hasta       fecha final o nula
     */
    public static void rangoFechas(List<Predicate> condiciones, CriteriaBuilder cb, Path<java.time.Instant> fecha,
                                   LocalDate desde, LocalDate hasta) {
        if (desde != null) {
            condiciones.add(cb.greaterThanOrEqualTo(fecha, desde.atStartOfDay(ZONA_NEGOCIO).toInstant()));
        }
        if (hasta != null) {
            condiciones.add(cb.lessThan(fecha, hasta.plusDays(1).atStartOfDay(ZONA_NEGOCIO).toInstant()));
        }
    }
}
