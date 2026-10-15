/*
 * Nombre:                 Compra.java
 * Descripción:            Entidad JPA de una compra a proveedor con sus líneas (tablas compra y compra_linea).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.8.0
 */
package com.ferrestock.compras.entidad;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.ferrestock.compartido.documentos.EstadoDocumento;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Compra recibida completa (RF-COM-002). Solo cambian los campos de anulación (RF-COM-005); las líneas no se
 * modifican.
 */
@Entity
@Table(name = "compra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20, updatable = false)
    private String consecutivo;

    @Column(name = "proveedor_id", nullable = false, updatable = false)
    private Long proveedorId;

    @Column(nullable = false, updatable = false)
    private Instant fecha;

    @Column(length = 300, updatable = false)
    private String observacion;

    @Column(nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private EstadoDocumento estado = EstadoDocumento.REGISTRADA;

    @Column(name = "motivo_anulacion", length = 300)
    private String motivoAnulacion;

    @Column(name = "anulada_por_id")
    private Long anuladaPorId;

    @Column(name = "anulada_en")
    private Instant anuladaEn;

    @Column(name = "creado_por_id", nullable = false, updatable = false)
    private Long creadoPorId;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.PERSIST)
    @OrderBy("id")
    @Builder.Default
    private List<CompraLinea> lineas = new ArrayList<>();

    /**
     * @param linea línea a agregar (fija la relación inversa)
     */
    public void agregar(CompraLinea linea) {
        linea.setCompra(this);
        lineas.add(linea);
    }
}
