/*
 * Nombre:                 ValidadorNit.java
 * Descripción:            Cálculo y verificación del dígito de verificación del NIT colombiano (algoritmo módulo 11 de la DIAN).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.5.0
 */
package com.ferrestock.compartido.validacion;

/**
 * Dígito de verificación (DV) del NIT. Cada dígito, de derecha a izquierda, se multiplica por los factores
 * 3, 7, 13, 17, 19, 23, 29, 37, 41, 43, 47, 53, 59, 67, 71; si el residuo de la suma entre 11 es 0 o 1, ese es el DV;
 * si no, el DV es 11 menos el residuo. Verificado con NIT públicos (p. ej. 890903938-8, 899999068-1).
 */
public final class ValidadorNit {

    private static final int[] FACTORES = {3, 7, 13, 17, 19, 23, 29, 37, 41, 43, 47, 53, 59, 67, 71};

    private ValidadorNit() {
    }

    /**
     * @param numero NIT sin dígito de verificación, solo dígitos (hasta 15)
     * @return dígito de verificación de 0 a 9
     * @throws IllegalArgumentException si el número no tiene entre 1 y 15 dígitos
     */
    public static int calcularDigito(String numero) {
        if (numero == null || !numero.matches("\\d{1,15}")) {
            throw new IllegalArgumentException("El NIT debe tener entre 1 y 15 dígitos");
        }
        int suma = 0;
        for (int i = 0; i < numero.length(); i++) {
            int digito = numero.charAt(numero.length() - 1 - i) - '0';
            suma += digito * FACTORES[i];
        }
        int residuo = suma % 11;
        return residuo < 2 ? residuo : 11 - residuo;
    }

    /**
     * @param numero              NIT sin dígito de verificación
     * @param digitoVerificacion dígito informado
     * @return {@code true} si el dígito corresponde al número
     */
    public static boolean esValido(String numero, String digitoVerificacion) {
        if (digitoVerificacion == null || !digitoVerificacion.matches("\\d")) {
            return false;
        }
        return calcularDigito(numero) == digitoVerificacion.charAt(0) - '0';
    }
}
