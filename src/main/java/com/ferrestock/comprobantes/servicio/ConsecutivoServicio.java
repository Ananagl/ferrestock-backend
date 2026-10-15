/*
 * Nombre:                 ConsecutivoServicio.java
 * Descripción:            Asigna consecutivos internos únicos y sin huecos por tipo de documento (RF-DOC-001).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.6.0
 */
package com.ferrestock.comprobantes.servicio;

import com.ferrestock.compartido.documentos.TipoDocumentoComercial;
import com.ferrestock.comprobantes.entidad.Consecutivo;
import com.ferrestock.comprobantes.repositorio.ConsecutivoRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consecutivos con el formato {@code PREFIJO-000123}. Exige la transacción del documento ({@code MANDATORY}):
 * si el documento no se guarda, el número tampoco se consume (DP-11, RNP-07).
 */
@Service
@RequiredArgsConstructor
public class ConsecutivoServicio {

    /** Dígitos del número (con ceros a la izquierda). */
    private static final int DIGITOS = 6;

    private final ConsecutivoRepositorio consecutivos;

    /**
     * @param tipo tipo de documento
     * @return siguiente consecutivo, por ejemplo {@code VEN-000001}
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public String siguiente(TipoDocumentoComercial tipo) {
        Consecutivo contador = consecutivos.bloquear(tipo.name())
            .orElseThrow(() -> new IllegalStateException("No hay contador para " + tipo + " (revise db/02_datos_iniciales.sql)"));
        long numero = contador.getSiguiente();
        contador.setSiguiente(numero + 1);
        return contador.getPrefijo() + "-" + String.format("%0" + DIGITOS + "d", numero);
    }
}
