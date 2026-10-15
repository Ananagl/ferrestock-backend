/*
 * Nombre:                 EscritorProblemas.java
 * Descripción:            Construye ProblemDetail traducidos y los escribe directamente en la respuesta desde los filtros de seguridad.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.2.0
 */
package com.ferrestock.compartido.error;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

/**
 * Fábrica única de respuestas de error (RFC 9457), usada por el manejador global y por los filtros de seguridad,
 * para que todos los errores tengan el mismo formato y estén traducidos.
 */
@Component
@RequiredArgsConstructor
public class EscritorProblemas {

    /** Prefijo de los URI de tipo de error. */
    public static final String URI_ERRORES = "https://ferrestock.app/errores/";

    private final MessageSource mensajes;
    private final ObjectMapper json;
    private final LocaleResolver idiomas;

    /**
     * Crea un ProblemDetail con título y detalle traducidos.
     *
     * @param codigo     código del error
     * @param idioma     idioma de la respuesta
     * @param argumentos argumentos del detalle
     * @return problema listo para devolver
     */
    public ProblemDetail crear(CodigoError codigo, Locale idioma, Object... argumentos) {
        ProblemDetail problema = ProblemDetail.forStatus(codigo.getEstado());
        problema.setType(URI.create(URI_ERRORES + codigo.name()));
        problema.setTitle(mensajes.getMessage("error." + codigo.name() + ".titulo", null, codigo.name(), idioma));
        problema.setDetail(mensajes.getMessage("error." + codigo.name() + ".detalle", argumentos, null, idioma));
        problema.setProperty("codigo", codigo.name());
        return problema;
    }

    /**
     * Escribe el problema en la respuesta HTTP. Se usa donde no llega el manejador de Spring MVC (filtros).
     *
     * @param solicitud petición en curso (define idioma e instancia)
     * @param respuesta respuesta HTTP
     * @param codigo    código del error
     * @throws IOException si no se puede escribir la respuesta
     */
    public void escribir(HttpServletRequest solicitud, HttpServletResponse respuesta, CodigoError codigo)
            throws IOException {
        ProblemDetail problema = crear(codigo, idiomas.resolveLocale(solicitud));
        problema.setInstance(URI.create(solicitud.getRequestURI()));
        respuesta.setStatus(problema.getStatus());
        respuesta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        json.writeValue(respuesta.getOutputStream(), problema);
    }
}
