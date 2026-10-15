/*
 * Nombre:                 Devolucion.java
 * Descripción:            Entidad JPA inmutable de una devolución de cliente con sus líneas (tablas devolucion y devolucion_linea).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.9.0
 */
package com.ferrestock.devoluciones.entidad;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.Immutable;

/**
 * Devolución de cliente (RF-DEV-001). Se valida al registrarla: {@code validadaPorId} es quien marcó cada línea
 * como aceptada o rechazada (RN-11). La venta es opcional.
 */
@Entity
@Immutable
@Table(name = "devolucion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Devolucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String consecutivo;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "venta_id")
    private Long ventaId;

    @Column(nullable = false, length = 300)
    private String motivo;

    @Column(nullable = false)
    private Instant fecha;

    @Column(name = "validada_por_id", nullable = false)
    private Long validadaPorId;

    @Column(name = "creado_por_id", nullable = false)
    private Long creadoPorId;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @OneToMany(mappedBy = "devolucion", cascade = CascadeType.PERSIST)
    @OrderBy("id")
    @Builder.Default
    private List<DevolucionLinea> lineas = new ArrayList<>();

    /**
     * @param linea línea a agregar (fija la relación inversa)
     */
    public void agregar(DevolucionLinea linea) {
        linea.setDevolucion(this);
        lineas.add(linea);
    }
}
