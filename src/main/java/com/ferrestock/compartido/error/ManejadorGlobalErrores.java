/*
 * Nombre:                 ManejadorGlobalErrores.java
 * Descripción:            Traduce las excepciones a respuestas ProblemDetail (RFC 9457) internacionalizadas, sin exponer detalles internos; los errores inesperados llevan un identificador de correlación.
 * Fecha de creación:      2026-10-14
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.2.0
 */
package com.ferrestock.compartido.error;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.ferrestock.auditoria.entidad.AccionAuditoria;
import com.ferrestock.auditoria.servicio.AuditoriaServicio;
import com.ferrestock.compartido.seguridad.UsuarioActual;
import com.ferrestock.compartido.seguridad.UsuarioAutenticado;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Manejo global de errores (RNF-FIA-03). Diseño: {@code docs/fase-2-diseno/04_api_rest.md}, sección 3.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ManejadorGlobalErrores {

    private final EscritorProblemas escritor;
    private final AuditoriaServicio auditoria;

    /**
     * Regla de negocio incumplida: estado y mensaje según el {@link CodigoError}; si hay campo, se agrega a
     * {@code errores} para que el formulario lo muestre junto al campo.
     *
     * @param ex     excepción de negocio
     * @param idioma idioma de la petición
     * @return problema traducido
     */
    @ExceptionHandler(NegocioException.class)
    public ProblemDetail negocio(NegocioException ex, Locale idioma) {
        ProblemDetail problema = escritor.crear(ex.getCodigo(), idioma, ex.getArgumentos());
        if (ex.getCampo() != null) {
            problema.setProperty("errores", List.of(Map.of("campo", ex.getCampo(),
                "mensaje", problema.getDetail() == null ? "" : problema.getDetail())));
        }
        return problema;
    }

    /**
     * Errores de validación de los DTO de entrada: 400 con un mensaje por campo.
     *
     * @param ex     excepción de validación
     * @param idioma idioma de la petición
     * @return problema con código {@code VALIDACION} y la lista de errores
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacion(MethodArgumentNotValidException ex, Locale idioma) {
        ProblemDetail problema = escritor.crear(CodigoError.VALIDACION, idioma);
        List<Map<String, String>> errores = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> Map.of("campo", e.getField(),
                "mensaje", e.getDefaultMessage() == null ? "" : e.getDefaultMessage()))
            .toList();
        problema.setProperty("errores", errores);
        return problema;
    }

    /**
     * Cuerpo JSON ilegible o parámetro con tipo incorrecto: 400.
     *
     * @param ex     excepción de lectura o conversión
     * @param idioma idioma de la petición
     * @return problema con código {@code VALIDACION}
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ProblemDetail entradaIlegible(Exception ex, Locale idioma) {
        return escritor.crear(CodigoError.VALIDACION, idioma);
    }

    /**
     * Rol sin permiso para la operación (lanzado por {@code @PreAuthorize}): 403, y queda auditado.
     *
     * @param ex       excepción de acceso denegado
     * @param idioma   idioma de la petición
     * @param peticion petición, para registrar la ruta
     * @return problema con código {@code ACCESO_DENEGADO}
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail accesoDenegado(AccessDeniedException ex, Locale idioma, HttpServletRequest peticion) {
        UsuarioAutenticado usuario = UsuarioActual.obtener().orElse(null);
        auditoria.registrarIndependiente(usuario == null ? null : usuario.getId(),
            usuario == null ? null : usuario.getNombreUsuario(), AccionAuditoria.ACCESO_DENEGADO,
            "Acceso denegado a " + peticion.getMethod() + " " + peticion.getRequestURI());
        return escritor.crear(CodigoError.ACCESO_DENEGADO, idioma);
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
        ProblemDetail problema = escritor.crear(CodigoError.ERROR_INTERNO, idioma);
        problema.setProperty("correlacion", correlacion.toString());
        return problema;
    }
}
