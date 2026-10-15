/*
 * Nombre:                 RegistroError.java
 * Descripción:            Entidad JPA de un error no controlado de la aplicación (tabla registro_error).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.13.0
 */
package com.ferrestock.monitoreo.entidad;

import java.time.Instant;
import java.util.UUID;

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
import lombok.Setter;

/**
 * Error registrado con su identificador de correlación (RF-MON-006). {@code taigaRef} guarda el issue de Taiga si
 * el error se volvió recurrente y se reportó.
 */
@Entity
@Table(name = "registro_error")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID correlacion;

    @Column(nullable = false)
    private Instant fecha;

    @Column(length = 200)
    private String ruta;

    @Column(length = 7)
    private String metodo;

    @Column(nullable = false, length = 150)
    private String tipo;

    @Column(length = 500)
    private String mensaje;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "taiga_ref", length = 30)
    private String taigaRef;
}
