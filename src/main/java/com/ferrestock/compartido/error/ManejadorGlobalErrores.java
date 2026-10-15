/*
 * Nombre:                 ManejadorGlobalErrores.java
 * Descripción:            Traduce las excepciones a respuestas ProblemDetail (RFC 9457) internacionalizadas, sin
 *                         exponer detalles internos; los errores inesperados llevan un identificador de correlación.
 * Fecha de creación:      2026-10-14
 * Fecha de actualización: 2026-10-14
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.1.0
 */
package com.ferrestock.compartido.error;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Manejo global de errores (RNF-FIA-03). Diseño: {@code docs/fase-2-diseno/04_api_rest.md}, sección 3.
 *
 * <p>En la Fase 4 se agregan las excepciones de negocio ({@code NegocioException}) y el registro de
 * errores en la tabla {@code registro_error}.</p>
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ManejadorGlobalErrores {

    /** Prefijo de los URI de tipo de error. */
    private static final String URI_ERRORES = "https://ferrestock.app/errores/";

    private final MessageSource mensajes;

    /**
     * Errores de validación de los DTO de entrada: 400 con un mensaje por campo.
     *
     * @param ex     excepción de validación
     * @param idioma idioma de la petición
     * @return problema con código {@code VALIDACION} y la lista de errores
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacion(MethodArgumentNotValidException ex, Locale idioma) {
        ProblemDetail problema = crear(HttpStatus.BAD_REQUEST, "VALIDACION", idioma);
        List<Map<String, String>> errores = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> Map.of("campo", e.getField(),
                "mensaje", e.getDefaultMessage() == null ? "" : e.getDefaultMessage()))
            .toList();
        problema.setProperty("errores", errores);
        return problema;
    }

    /**
     * Rol sin permiso para la operación (lanzado por {@code @PreAuthorize}): 403.
     *
     * @param ex     excepción de acceso denegado
     * @param idioma idioma de la petición
     * @return problema con código {@code ACCESO_DENEGADO}
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail accesoDenegado(AccessDeniedException ex, Locale idioma) {
        return crear(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO", idioma);
    }

    /**
     * Cualquier error no previsto: 500 con un identificador de correlación y sin detalles internos.
     * Las excepciones HTTP propias de Spring (404, 405, 415...) conservan su código original.
     *
     * @param ex     excepción no controlada
     * @param idioma idioma de la petición
     * @return problema con código {@code ERROR_INTERNO}, o el de la excepción HTTP de Spring
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail inesperado(Exception ex, Locale idioma) {
        if (ex instanceof ErrorResponse respuestaHttp) {
            return respuestaHttp.getBody();
        }
        UUID correlacion = UUID.randomUUID();
        log.error("Error no controlado [correlacion={}]", correlacion, ex);
        ProblemDetail problema = crear(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO", idioma);
        problema.setProperty("correlacion", correlacion.toString());
        return problema;
    }

    /**
     * Construye un ProblemDetail con título y detalle traducidos desde {@code error.<CODIGO>.titulo/detalle}.
     */
    private ProblemDetail crear(HttpStatus estado, String codigo, Locale idioma) {
        ProblemDetail problema = ProblemDetail.forStatus(estado);
        problema.setType(URI.create(URI_ERRORES + codigo));
        problema.setTitle(mensajes.getMessage("error." + codigo + ".titulo", null, codigo, idioma));
        problema.setDetail(mensajes.getMessage("error." + codigo + ".detalle", null, null, idioma));
        problema.setProperty("codigo", codigo);
        return problema;
    }
}
