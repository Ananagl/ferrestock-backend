/*
 * Nombre:                 CompraLinea.java
 * Descripción:            Entidad JPA de una línea de compra: producto, cantidad, costo unitario y subtotal.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.8.0
 */
package com.ferrestock.compras.entidad;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * Línea de compra. Un producto aparece una sola vez por compra (DP-19).
 */
@Entity
@Table(name = "compra_linea")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompraLinea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compra_id", nullable = false, updatable = false)
    private Compra compra;

    @Column(name = "producto_id", nullable = false, updatable = false)
    private Long productoId;

    @Column(nullable = false, precision = 14, scale = 3, updatable = false)
    private BigDecimal cantidad;

    @Column(name = "costo_unitario", nullable = false, precision = 16, scale = 4, updatable = false)
    private BigDecimal costoUnitario;

    @Column(nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal subtotal;
}
