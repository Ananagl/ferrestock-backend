/*
 * Nombre:                 Auditoria.java
 * Descripción:            Entidad JPA inmutable de la tabla auditoria (solo inserción).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.2.0
 */
package com.ferrestock.auditoria.entidad;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Evento de auditoría (RN-11): usuario, fecha y hora, y descripción. Hibernate nunca la actualiza
 * ({@code @Immutable}) y la base de datos rechaza UPDATE y DELETE con un trigger.
 */
@Entity
@Immutable
@Table(name = "auditoria")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "nombre_usuario", nullable = false, length = 50)
    private String nombreUsuario;

    @Column(nullable = false)
    private Instant fecha;

    /**
     * Código de la acción. Se guarda como texto (no como enum JPA) para que un código histórico o renombrado no
     * impida leer la auditoría; los valores nuevos salen de {@link AccionAuditoria}.
     */
    @Column(nullable = false, length = 40)
    private String accion;

    @Column(nullable = false, length = 500)
    private String descripcion;
}
