/*
 * Nombre:                 Tercero.java
 * Descripción:            Entidad JPA de la tabla tercero: clientes y proveedores en un solo registro (ADR-06).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.5.0
 */
package com.ferrestock.terceros.entidad;

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
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * Cliente, proveedor o ambos (RN-10; 02_modelo_de_datos.md, sección 3.5). Sin relaciones JPA.
 */
@Entity
@Table(name = "tercero")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tercero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_persona", nullable = false, length = 10)
    private TipoPersona tipoPersona;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 5)
    private TipoDocumento tipoDocumento;

    @Column(name = "numero_documento", nullable = false, length = 20)
    private String numeroDocumento;

    /** Columna CHAR(1) en PostgreSQL; se indica el tipo JDBC para que la validación del esquema coincida. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "digito_verificacion", length = 1)
    private String digitoVerificacion;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 20)
    private String telefono;

    @Column(length = 120)
    private String correo;

    @Column(length = 200)
    private String direccion;

    @Column(name = "es_cliente", nullable = false)
    private boolean esCliente;

    @Column(name = "es_proveedor", nullable = false)
    private boolean esProveedor;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;

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
