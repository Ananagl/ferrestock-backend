/*
 * Nombre:                 MovimientoInventarioRepositorio.java
 * Descripción:            Acceso a datos del kardex: consultas filtradas y verificación de consistencia.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.6.0
 */
package com.ferrestock.inventario.repositorio;

import java.math.BigDecimal;
import java.util.List;

import com.ferrestock.inventario.entidad.MovimientoInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/**
 * Repositorio del kardex (solo inserción y lectura).
 */
public interface MovimientoInventarioRepositorio
    extends JpaRepository<MovimientoInventario, Long>, JpaSpecificationExecutor<MovimientoInventario> {

    /**
     * Fila de la verificación de consistencia.
     */
    interface Inconsistencia {
        /** @return id del producto */
        Long getProductoId();

        /** @return código del producto */
        String getCodigo();

        /** @return existencias guardadas en el producto */
        BigDecimal getExistencias();

        /** @return entradas menos salidas según el kardex */
        BigDecimal getExistenciasKardex();

        /** @return costo promedio guardado en el producto */
        BigDecimal getCostoPromedio();

        /** @return costo promedio resultante del último movimiento, o nulo si no hay movimientos */
        BigDecimal getCostoPromedioKardex();
    }

    /**
     * Productos cuyas existencias o costo no coinciden con el kardex (03_inventario_y_costos.md, sección 8).
     * Un resultado vacío significa que todo es consistente.
     *
     * @return inconsistencias encontradas
     */
    @Query(nativeQuery = true, value = """
        SELECT p.id AS producto_id, p.codigo AS codigo, p.existencias AS existencias,
               COALESCE(k.saldo, 0) AS existencias_kardex, p.costo_promedio AS costo_promedio,
               u.costo_promedio_resultante AS costo_promedio_kardex
        FROM producto p
        LEFT JOIN (SELECT producto_id,
                          SUM(CASE WHEN naturaleza = 'ENTRADA' THEN cantidad ELSE -cantidad END) AS saldo
                   FROM movimiento_inventario GROUP BY producto_id) k ON k.producto_id = p.id
        LEFT JOIN LATERAL (SELECT costo_promedio_resultante FROM movimiento_inventario m
                           WHERE m.producto_id = p.id ORDER BY m.id DESC LIMIT 1) u ON TRUE
        WHERE p.existencias <> COALESCE(k.saldo, 0)
           OR (u.costo_promedio_resultante IS NOT NULL AND p.costo_promedio <> u.costo_promedio_resultante)
        ORDER BY p.codigo""")
    List<Inconsistencia> buscarInconsistencias();
}
