/*
 * Nombre:                 Ajuste.java
 * Descripción:            Entidad JPA inmutable de un ajuste de inventario (tabla ajuste).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.7.0
 */
package com.ferrestock.ajustes.entidad;

import java.math.BigDecimal;
import java.time.Instant;

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
 * Ajuste de inventario (02_modelo_de_datos.md, 3.9). Es un documento con consecutivo {@code AJU-…}: no se modifica
 * ni se elimina (RNP-05). En las salidas, {@code costoUnitario} es el CPP con que salió la mercancía.
 */
@Entity
@Immutable
@Table(name = "ajuste")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ajuste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String consecutivo;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoAjuste tipo;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidad;

    @Column(name = "costo_unitario", nullable = false, precision = 16, scale = 4)
    private BigDecimal costoUnitario;

    @Column(nullable = false, length = 300)
    private String motivo;

    @Column(nullable = false)
    private Instant fecha;

    @Column(name = "creado_por_id", nullable = false)
    private Long creadoPorId;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;
}
