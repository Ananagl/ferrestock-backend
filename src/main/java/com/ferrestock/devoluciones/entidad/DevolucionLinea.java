/*
 * Nombre:                 DevolucionLinea.java
 * Descripción:            Entidad JPA inmutable de una línea de devolución: producto, cantidad, resultado y costo de reingreso.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.9.0
 */
package com.ferrestock.devoluciones.entidad;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

/**
 * Línea de devolución. {@code costoUnitario} solo existe en las aceptadas: el costo histórico de la línea vendida
 * o, sin venta, el CPP vigente (DP-08).
 */
@Entity
@Immutable
@Table(name = "devolucion_linea")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DevolucionLinea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "devolucion_id", nullable = false)
    private Devolucion devolucion;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "venta_linea_id")
    private Long ventaLineaId;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ResultadoLinea resultado;

    @Column(name = "costo_unitario", precision = 16, scale = 4)
    private BigDecimal costoUnitario;
}
