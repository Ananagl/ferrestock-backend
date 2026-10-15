/*
 * Nombre:                 ValidadorContrasenaSegura.java
 * Descripción:            Implementación de la regla @ContrasenaSegura.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.2.0
 */
package com.ferrestock.compartido.validacion;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Valida la política de contraseñas. Un valor nulo se considera válido: la obligatoriedad la controla
 * {@code @NotBlank}.
 */
public class ValidadorContrasenaSegura implements ConstraintValidator<ContrasenaSegura, String> {

    /** Longitud mínima. */
    public static final int MINIMO = 8;

    /** Longitud máxima admitida por BCrypt. */
    public static final int MAXIMO = 72;

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        if (valor == null) {
            return true;
        }
        return valor.length() >= MINIMO
            && valor.length() <= MAXIMO
            && valor.chars().anyMatch(Character::isLetter)
            && valor.chars().anyMatch(Character::isDigit);
    }
}
