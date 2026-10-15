/*
 * Nombre:                 Consecutivo.java
 * Descripción:            Entidad JPA de la tabla consecutivo: siguiente número por tipo de documento.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.6.0
 */
package com.ferrestock.comprobantes.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Contador de consecutivos (ADR-09). Se lee con bloqueo de fila dentro de la transacción del documento, así un
 * fallo no consume el número y no quedan huecos.
 */
@Entity
@Table(name = "consecutivo")
@Getter
@Setter
@NoArgsConstructor
public class Consecutivo {

    @Id
    @Column(name = "tipo_documento", length = 12)
    private String tipoDocumento;

    @Column(nullable = false, length = 5)
    private String prefijo;

    @Column(nullable = false)
    private Long siguiente;
}
