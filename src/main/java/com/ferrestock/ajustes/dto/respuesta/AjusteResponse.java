/*
 * Nombre:                 AjusteResponse.java
 * Descripción:            DTO de salida de un ajuste de inventario (RF-AJU-004).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.7.0
 */
package com.ferrestock.ajustes.dto.respuesta;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * @param id                      ajuste
 * @param consecutivo             número del documento, por ejemplo {@code AJU-000001}
 * @param fecha                   fecha y hora (UTC)
 * @param productoId              producto
 * @param productoCodigo          código del producto
 * @param productoNombre          nombre del producto
 * @param tipo                    ENTRADA o SALIDA
 * @param cantidad                cantidad ajustada
 * @param costoUnitario           costo de la entrada o CPP con que salió
 * @param motivo                  motivo documentado
 * @param usuario                 nombre de usuario responsable
 * @param existenciasResultantes  existencias después del ajuste (solo en la respuesta al registrarlo)
 * @param costoPromedioResultante CPP después del ajuste (solo en la respuesta al registrarlo)
 */
public record AjusteResponse(Long id, String consecutivo, Instant fecha, Long productoId, String productoCodigo,
                             String productoNombre, String tipo, BigDecimal cantidad, BigDecimal costoUnitario,
                             String motivo, String usuario, BigDecimal existenciasResultantes,
                             BigDecimal costoPromedioResultante) {
}
