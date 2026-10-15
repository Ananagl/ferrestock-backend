/*
 * Nombre:                 RegistroErrorServicio.java
 * Descripción:            Registra los errores no controlados y reporta a Taiga los recurrentes: el mismo tipo en la misma ruta 5 o más veces en 10 minutos (RF-MON-006).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.13.0
 */
package com.ferrestock.monitoreo.servicio;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.ferrestock.monitoreo.entidad.RegistroError;
import com.ferrestock.monitoreo.repositorio.RegistroErrorRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro de errores. Se guarda en una transacción propia para que quede aunque la operación del usuario falle.
 */
@Service
@RequiredArgsConstructor
public class RegistroErrorServicio {

    private final RegistroErrorRepositorio errores;
    private final ReporteTaigaServicio taiga;
    private final Clock reloj;

    @Value("${ferrestock.monitoreo.umbral-recurrencia:5}")
    private int umbral;
    @Value("${ferrestock.monitoreo.ventana-minutos:10}")
    private int ventanaMinutos;

    /**
     * @param correlacion identificador que recibe el usuario
     * @param metodo      método HTTP
     * @param ruta        ruta de la petición
     * @param error       excepción
     * @param usuarioId   usuario autenticado o nulo
     * @return el registro guardado (con la referencia de Taiga si se reportó)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RegistroError registrar(UUID correlacion, String metodo, String ruta, Throwable error, Long usuarioId) {
        Instant ahora = reloj.instant();
        String tipo = recortar(error.getClass().getName(), 150);
        String rutaCorta = recortar(ruta, 200);
        RegistroError registro = errores.save(RegistroError.builder().correlacion(correlacion).fecha(ahora).metodo(metodo)
            .ruta(rutaCorta).tipo(tipo).mensaje(recortar(error.getMessage(), 500)).usuarioId(usuarioId).build());
        Instant inicioVentana = ahora.minus(Duration.ofMinutes(ventanaMinutos));
        if (errores.countByTipoAndRutaAndFechaAfter(tipo, rutaCorta, inicioVentana) >= umbral
            && !errores.existsByTipoAndRutaAndFechaAfterAndTaigaRefIsNotNull(tipo, rutaCorta, inicioVentana)) {
            taiga.crearIssue("[Producción] " + tipo.substring(tipo.lastIndexOf('.') + 1) + " en " + metodo + " " + rutaCorta,
                    "El error se repitió " + umbral + " o más veces en " + ventanaMinutos + " minutos.\n\n**Tipo:** " + tipo
                        + "\n**Ruta:** " + metodo + " " + rutaCorta + "\n**Última correlación:** " + correlacion
                        + "\n**Mensaje:** " + registro.getMensaje())
                .ifPresent(registro::setTaigaRef);
        }
        return registro;
    }

    private static String recortar(String texto, int maximo) {
        return texto == null || texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }
}
