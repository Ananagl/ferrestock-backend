/*
 * Nombre:                 MovimientoInventario.java
 * Descripción:            Entidad JPA inmutable del kardex (tabla movimiento_inventario).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.6.0
 */
package com.ferrestock.inventario.entidad;

import java.math.BigDecimal;
import java.time.Instant;

import com.ferrestock.compartido.documentos.TipoDocumentoComercial;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Movimiento de inventario (RF-INV-002). Guarda el saldo y el CPP resultantes, y el CPP anterior para poder
 * revertir anulaciones (03_inventario_y_costos.md, sección 5). Nunca se modifica ni se borra (trigger ADR-15).
 */
@Entity
@Immutable
@Table(name = "movimiento_inventario")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private TipoMovimiento tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 7)
    private Naturaleza naturaleza;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidad;

    @Column(name = "costo_unitario", nullable = false, precision = 16, scale = 4)
    private BigDecimal costoUnitario;

    @Column(name = "saldo_resultante", nullable = false, precision = 14, scale = 3)
    private BigDecimal saldoResultante;

    @Column(name = "costo_promedio_anterior", nullable = false, precision = 16, scale = 4)
    private BigDecimal costoPromedioAnterior;

    @Column(name = "costo_promedio_resultante", nullable = false, precision = 16, scale = 4)
    private BigDecimal costoPromedioResultante;

    @Enumerated(EnumType.STRING)
    @Column(name = "documento_tipo", nullable = false, length = 12)
    private TipoDocumentoComercial documentoTipo;

    @Column(name = "documento_id", nullable = false)
    private Long documentoId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private Instant fecha;
}
