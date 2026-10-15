/*
 * Nombre:                 CodigoError.java
 * Descripción:            Catálogo de códigos de error estables de la API con su estado HTTP (04_api_rest.md, sección 3).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.4.0
 */
package com.ferrestock.compartido.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Códigos de error que viajan en el campo {@code codigo} del ProblemDetail. Sus textos están en
 * {@code i18n/mensajes*.properties} con las claves {@code error.<CODIGO>.titulo} y {@code error.<CODIGO>.detalle}.
 */
@Getter
@RequiredArgsConstructor
public enum CodigoError {
    /** Datos de entrada inválidos. */
    VALIDACION(HttpStatus.BAD_REQUEST),
    /** Usuario o contraseña incorrectos (mensaje genérico). */
    CREDENCIALES_INVALIDAS(HttpStatus.UNAUTHORIZED),
    /** Petición sin sesión válida. */
    NO_AUTENTICADO(HttpStatus.UNAUTHORIZED),
    /** Cuenta bloqueada temporalmente por intentos fallidos. */
    CUENTA_BLOQUEADA(HttpStatus.LOCKED),
    /** El rol no tiene permiso, o el token CSRF es inválido. */
    ACCESO_DENEGADO(HttpStatus.FORBIDDEN),
    /** El usuario debe cambiar su contraseña temporal antes de continuar. */
    CAMBIO_CONTRASENA_REQUERIDO(HttpStatus.FORBIDDEN),
    /** La contraseña actual no coincide. */
    CONTRASENA_ACTUAL_INCORRECTA(HttpStatus.BAD_REQUEST),
    /** La confirmación no coincide con la nueva contraseña. */
    CONTRASENA_NO_COINCIDE(HttpStatus.BAD_REQUEST),
    /** La nueva contraseña es igual a la actual. */
    CONTRASENA_REPETIDA(HttpStatus.BAD_REQUEST),
    /** La nueva contraseña contiene el nombre de usuario. */
    CONTRASENA_CONTIENE_USUARIO(HttpStatus.BAD_REQUEST),
    /** Recurso inexistente. */
    NO_ENCONTRADO(HttpStatus.NOT_FOUND),
    /** Ya existe un registro con ese valor único. */
    DUPLICADO(HttpStatus.CONFLICT),
    /** Un administrador no puede desactivarse, cambiarse el rol ni restablecerse la contraseña a sí mismo. */
    OPERACION_SOBRE_SI_MISMO(HttpStatus.CONFLICT),
    /** Otro usuario modificó el registro después de que se cargó (bloqueo optimista). */
    VERSION_DESACTUALIZADA(HttpStatus.CONFLICT),
    /** La categoría está inactiva y no se puede asignar. */
    CATEGORIA_INACTIVA(HttpStatus.CONFLICT),
    /** El producto está inactivo y no admite nuevas operaciones (RF-PRO-008). */
    PRODUCTO_INACTIVO(HttpStatus.CONFLICT),
    /** Error inesperado del servidor. */
    ERROR_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR);

    /** Estado HTTP asociado. */
    private final HttpStatus estado;
}
