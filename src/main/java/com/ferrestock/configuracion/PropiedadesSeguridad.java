/*
 * Nombre:                 PropiedadesSeguridad.java
 * Descripción:            Parámetros de seguridad configurables (ferrestock.seguridad.*).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.2.0
 */
package com.ferrestock.configuracion;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propiedades de seguridad. Los valores por defecto están en {@code application.yml}; la aplicación no arranca si
 * falta la clave de "Recordarme".
 *
 * @param swaggerPublico  si la documentación OpenAPI se ve sin sesión (solo dev)
 * @param intentosMaximos intentos fallidos antes de bloquear la cuenta (RF-AUT-008)
 * @param minutosBloqueo  duración del bloqueo temporal
 * @param recuerdameClave clave para firmar los tokens de "Recordarme" (variable FS_RECUERDAME_CLAVE)
 * @param recuerdameDias  validez de la sesión persistente (RNF-SEG-04)
 * @param cookiesSeguras  si las cookies llevan el atributo Secure (producción con HTTPS)
 */
@Validated
@ConfigurationProperties(prefix = "ferrestock.seguridad")
public record PropiedadesSeguridad(
    boolean swaggerPublico,
    @Min(1) int intentosMaximos,
    @Min(1) int minutosBloqueo,
    @NotBlank String recuerdameClave,
    @Min(1) int recuerdameDias,
    boolean cookiesSeguras) {
}
