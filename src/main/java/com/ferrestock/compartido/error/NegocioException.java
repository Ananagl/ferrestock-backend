/*
 * Nombre:                 NegocioException.java
 * Descripción:            Excepción de negocio con código estable, argumentos para el mensaje y campo opcional del formulario.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.6.0
 */
package com.ferrestock.compartido.error;

import lombok.Getter;

/**
 * Excepción que los servicios lanzan cuando una regla de negocio impide la operación. El manejador global la
 * convierte en un ProblemDetail traducido; si indica un {@code campo}, el frontend muestra el error junto a ese campo.
 */
@Getter
public class NegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Código estable del error. */
    private final CodigoError codigo;

    /** Campo del formulario al que se refiere el error, o {@code null}. */
    private final String campo;

    /** Argumentos para el mensaje traducido. */
    private final transient Object[] argumentos;

    /**
     * Crea una excepción sin campo asociado.
     *
     * @param codigo     código del error
     * @param argumentos argumentos del mensaje
     */
    public NegocioException(CodigoError codigo, Object... argumentos) {
        this(codigo, null, argumentos);
    }

    /**
     * Crea una excepción asociada a un campo del formulario. Es una fábrica (y no un constructor público) para que
     * un argumento de texto del mensaje nunca se confunda con el nombre del campo.
     *
     * @param codigo     código del error
     * @param campo      nombre del campo en el DTO de entrada
     * @param argumentos argumentos del mensaje
     * @return la excepción
     */
    public static NegocioException enCampo(CodigoError codigo, String campo, Object... argumentos) {
        return new NegocioException(codigo, campo, argumentos);
    }

    private NegocioException(CodigoError codigo, String campo, Object[] argumentos) {
        super(codigo.name());
        this.codigo = codigo;
        this.campo = campo;
        this.argumentos = argumentos;
    }
}
