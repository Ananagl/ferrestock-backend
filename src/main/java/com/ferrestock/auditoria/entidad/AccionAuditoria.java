/*
 * Nombre:                 AccionAuditoria.java
 * Descripción:            Códigos estables de las acciones auditadas.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.8.0
 */
package com.ferrestock.auditoria.entidad;

/**
 * Acciones registradas en la auditoría. Cada incremento agrega las de su módulo.
 */
public enum AccionAuditoria {
    /** Inicio de sesión correcto. */
    LOGIN_EXITOSO,
    /** Intento de inicio de sesión fallido. */
    LOGIN_FALLIDO,
    /** La cuenta se bloqueó por intentos fallidos. */
    CUENTA_BLOQUEADA,
    /** Cierre de sesión. */
    LOGOUT,
    /** Cambio de contraseña propio. */
    CONTRASENA_CAMBIADA,
    /** Intento de usar un endpoint sin el rol requerido. */
    ACCESO_DENEGADO,
    /** Alta de un usuario (RF-AUT-006). */
    USUARIO_CREADO,
    /** Cambio del nombre completo de un usuario. */
    USUARIO_ACTUALIZADO,
    /** Cambio del rol de un usuario. */
    ROL_CAMBIADO,
    /** Desactivación de un usuario. */
    USUARIO_DESACTIVADO,
    /** Reactivación de un usuario. */
    USUARIO_ACTIVADO,
    /** Contraseña temporal asignada por el administrador. */
    CONTRASENA_RESTABLECIDA,
    /** Alta de categoría (RF-PRO-009). */
    CATEGORIA_CREADA,
    /** Cambio de nombre de categoría. */
    CATEGORIA_ACTUALIZADA,
    /** Activación o desactivación de categoría. */
    CATEGORIA_ESTADO_CAMBIADO,
    /** Alta de producto (RF-PRO-001). */
    PRODUCTO_CREADO,
    /** Cambio de datos descriptivos de producto (RF-PRO-005). */
    PRODUCTO_ACTUALIZADO,
    /** Desactivación de producto (RF-PRO-006). */
    PRODUCTO_DESACTIVADO,
    /** Reactivación de producto (RF-PRO-007). */
    PRODUCTO_ACTIVADO,
    /** Cambio de precio de venta (RF-PRE-002, RN-09). */
    PRECIO_MODIFICADO,
    /** Alta de cliente o proveedor (RF-TER-001). */
    TERCERO_CREADO,
    /** Cambio de datos de un tercero (RF-TER-005). */
    TERCERO_ACTUALIZADO,
    /** Desactivación de un tercero (RF-TER-006). */
    TERCERO_DESACTIVADO,
    /** Reactivación de un tercero (RF-TER-006). */
    TERCERO_ACTIVADO,
    /** Ajuste de inventario registrado por el Administrador (RF-AJU-001, RN-08). */
    AJUSTE_REGISTRADO,
    /** Compra registrada y recibida (RF-COM-001). */
    COMPRA_REGISTRADA,
    /** Compra anulada por el Administrador (RF-COM-005). */
    COMPRA_ANULADA,
    /** Venta y pago confirmados (RF-VEN-001, RF-PAG-003). */
    VENTA_REGISTRADA,
    /** Venta anulada por el Administrador (RF-VEN-006). */
    VENTA_ANULADA,
    /** Devolución vinculada a una venta (RF-DEV-001). */
    DEVOLUCION_REGISTRADA,
    /** Devolución sin venta vinculada, para revisión del Administrador (RF-DEV-004). */
    DEVOLUCION_SIN_VENTA
}
