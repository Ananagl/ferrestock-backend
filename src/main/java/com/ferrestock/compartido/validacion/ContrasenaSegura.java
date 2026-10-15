/*
 * Nombre:                 ContrasenaSegura.java
 * Descripción:            Anotación de Bean Validation para la política de contraseñas (RNF-SEG-01).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.2.0
 */
package com.ferrestock.compartido.validacion;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * La contraseña debe tener entre 8 y 72 caracteres, al menos una letra y al menos un número.
 * El límite de 72 se debe a BCrypt, que ignora los bytes posteriores.
 */
@Documented
@Constraint(validatedBy = ValidadorContrasenaSegura.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ContrasenaSegura {

    /** @return clave del mensaje de error */
    String message() default "{validacion.contrasena.insegura}";

    /** @return grupos de validación */
    Class<?>[] groups() default {};

    /** @return carga útil */
    Class<? extends Payload>[] payload() default {};
}
