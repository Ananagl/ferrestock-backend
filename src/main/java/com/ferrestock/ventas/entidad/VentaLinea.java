/*
 * Nombre:                 VentaLinea.java
 * Descripción:            Entidad JPA de una línea de venta: precio vigente, costo al momento de la venta y cantidad devuelta.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.8.0
 */
package com.ferrestock.ventas.entidad;

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
 * Línea de venta. {@code costoUnitario} es el CPP con que salió (RF-COS-004): se usa para la anulación y para las
 * devoluciones con venta (DP-08). {@code cantidadDevuelta} acumula las devoluciones aceptadas (RNP-04).
 */
@Entity
@Table(name = "venta_linea")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VentaLinea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false, updatable = false)
    private Venta venta;

    @Column(name = "producto_id", nullable = false, updatable = false)
    private Long productoId;

    @Column(nullable = false, precision = 14, scale = 3, updatable = false)
    private BigDecimal cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal precioUnitario;

    @Column(name = "costo_unitario", nullable = false, precision = 16, scale = 4, updatable = false)
    private BigDecimal costoUnitario;

    @Column(nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal subtotal;

    @Column(name = "cantidad_devuelta", nullable = false, precision = 14, scale = 3)
    @Builder.Default
    private BigDecimal cantidadDevuelta = new BigDecimal("0.000");
}
