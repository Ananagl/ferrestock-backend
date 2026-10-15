/*
 * Nombre:                 Producto.java
 * Descripción:            Entidad JPA de la tabla producto, con bloqueo optimista para las ediciones.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.4.0
 */
package com.ferrestock.catalogo.entidad;

import java.math.BigDecimal;
import java.time.Instant;

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
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Producto del catálogo (02_modelo_de_datos.md, sección 3.4). Se usan {@code @Getter}/{@code @Setter} y no
 * {@code @Data}, porque la relación con {@link Categoria} haría que equals/hashCode/toString carguen datos
 * perezosos.
 *
 * <p>{@code existencias} y {@code costoPromedio} solo los modifica {@code MovimientoInventarioService}
 * (incremento 5); ningún DTO de entrada los incluye (ADR-07).</p>
 */
@Entity
@Table(name = "producto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidad_medida", nullable = false, length = 15)
    private UnidadMedida unidadMedida;

    @Column(name = "permite_fraccion", nullable = false)
    private boolean permiteFraccion;

    @Column(name = "precio_venta", nullable = false, precision = 14, scale = 2)
    private BigDecimal precioVenta;

    @Builder.Default
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal existencias = new BigDecimal("0.000");

    @Builder.Default
    @Column(name = "costo_promedio", nullable = false, precision = 16, scale = 4)
    private BigDecimal costoPromedio = new BigDecimal("0.0000");

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoProducto estado = EstadoProducto.ACTIVO;

    /** Versión para el bloqueo optimista (ADR-08). */
    @Version
    @Column(nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @Column(name = "creado_por_id", updatable = false)
    private Long creadoPorId;

    @Column(name = "actualizado_por_id")
    private Long actualizadoPorId;
}
