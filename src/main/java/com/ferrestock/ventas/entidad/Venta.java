/*
 * Nombre:                 Venta.java
 * Descripción:            Entidad JPA de una venta con su pago en efectivo y sus líneas (tablas venta y venta_linea).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.8.0
 */
package com.ferrestock.ventas.entidad;

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
 * Venta confirmada junto con su pago (DP-10, ADR-05): no existen ventas sin pagar. Solo cambian los campos de
 * anulación (RF-VEN-006) y, en las líneas, la cantidad devuelta.
 */
@Entity
@Table(name = "venta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20, updatable = false)
    private String consecutivo;

    @Column(name = "cliente_id", nullable = false, updatable = false)
    private Long clienteId;

    @Column(nullable = false, updatable = false)
    private Instant fecha;

    @Column(nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal total;

    @Column(name = "medio_pago", nullable = false, length = 10, updatable = false)
    @Builder.Default
    private String medioPago = "EFECTIVO";

    @Column(name = "valor_recibido", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal valorRecibido;

    @Column(nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal cambio;

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

    @OneToMany(mappedBy = "venta", cascade = CascadeType.PERSIST)
    @OrderBy("id")
    @Builder.Default
    private List<VentaLinea> lineas = new ArrayList<>();

    /**
     * @param linea línea a agregar (fija la relación inversa)
     */
    public void agregar(VentaLinea linea) {
        linea.setVenta(this);
        lineas.add(linea);
    }
}
