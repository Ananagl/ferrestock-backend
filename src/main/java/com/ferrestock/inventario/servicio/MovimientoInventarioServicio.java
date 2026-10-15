/*
 * Nombre:                 MovimientoInventarioServicio.java
 * Descripción:            Único punto que modifica existencias y costo promedio: registra entradas y salidas en el kardex (ADR-07).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.6.0
 */
package com.ferrestock.inventario.servicio;

import java.math.BigDecimal;
import java.time.Clock;

import com.ferrestock.catalogo.entidad.Producto;
import com.ferrestock.compartido.documentos.TipoDocumentoComercial;
import com.ferrestock.compartido.error.CodigoError;
import com.ferrestock.compartido.error.NegocioException;
import com.ferrestock.compartido.numeros.Montos;
import com.ferrestock.compartido.seguridad.UsuarioActual;
import com.ferrestock.compartido.seguridad.UsuarioAutenticado;
import com.ferrestock.inventario.entidad.MovimientoInventario;
import com.ferrestock.inventario.entidad.Naturaleza;
import com.ferrestock.inventario.entidad.TipoMovimiento;
import com.ferrestock.inventario.repositorio.MovimientoInventarioRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro de movimientos de inventario (03_inventario_y_costos.md). Contrato con los módulos que lo usan:
 * <ol>
 *   <li>Abren una transacción y bloquean los productos con {@code ProductoServicio.bloquearParaMovimiento}.</li>
 *   <li>Validan la cantidad con {@link #validarCantidad} y llaman a {@link #registrarEntrada} o
 *       {@link #registrarSalida}.</li>
 * </ol>
 * Los métodos exigen una transacción activa ({@code MANDATORY}): un movimiento nunca existe sin su documento.
 */
@Service
@RequiredArgsConstructor
public class MovimientoInventarioServicio {

    private final MovimientoInventarioRepositorio movimientos;
    private final Clock reloj;

    /**
     * Normaliza y valida una cantidad para un producto: mayor que cero, hasta 3 decimales y sin decimales si el
     * producto no se vende en fracciones (CA-PRO-09).
     *
     * @param producto producto de la operación
     * @param cantidad cantidad solicitada
     * @param campo    campo del formulario al que se asocia el error
     * @return cantidad con 3 decimales
     * @throws NegocioException {@code CANTIDAD_INVALIDA} o {@code CANTIDAD_FRACCIONARIA_NO_PERMITIDA}
     */
    public BigDecimal validarCantidad(Producto producto, BigDecimal cantidad, String campo) {
        if (cantidad == null || cantidad.signum() <= 0 || cantidad.stripTrailingZeros().scale() > Montos.ESCALA_CANTIDAD) {
            throw NegocioException.enCampo(CodigoError.CANTIDAD_INVALIDA, campo);
        }
        if (!producto.isPermiteFraccion() && !Montos.esEntera(cantidad)) {
            throw NegocioException.enCampo(CodigoError.CANTIDAD_FRACCIONARIA_NO_PERMITIDA, campo, producto.getCodigo());
        }
        return Montos.cantidad(cantidad);
    }

    /**
     * Registra una entrada. Si {@code pondera} es verdadero, recalcula el CPP con la fórmula ponderada (compras,
     * ajustes de entrada, devoluciones con venta, anulaciones de venta); si no, la entrada se valora al CPP vigente
     * y el CPP no cambia (devoluciones sin venta).
     *
     * @param producto      producto bloqueado
     * @param cantidad      cantidad validada
     * @param costoUnitario costo unitario de la entrada (se ignora si {@code pondera} es falso)
     * @param tipo          tipo de movimiento de naturaleza ENTRADA
     * @param pondera       si la entrada modifica el CPP
     * @param documentoTipo documento origen
     * @param documentoId   id del documento origen
     * @return movimiento guardado
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public MovimientoInventario registrarEntrada(Producto producto, BigDecimal cantidad, BigDecimal costoUnitario,
                                                 TipoMovimiento tipo, boolean pondera,
                                                 TipoDocumentoComercial documentoTipo, Long documentoId) {
        exigirNaturaleza(tipo, Naturaleza.ENTRADA);
        BigDecimal anterior = producto.getCostoPromedio();
        BigDecimal costoMovimiento = pondera ? Montos.costo(costoUnitario) : anterior;
        BigDecimal nuevoCosto = pondera
            ? Montos.costoPromedioPonderado(producto.getExistencias(), anterior, cantidad, costoMovimiento)
            : anterior;
        BigDecimal saldo = Montos.cantidad(producto.getExistencias().add(cantidad));
        return aplicar(producto, tipo, cantidad, costoMovimiento, saldo, anterior, nuevoCosto, documentoTipo, documentoId);
    }

    /**
     * Registra una salida al CPP vigente (el CPP no cambia). Rechaza la operación si no hay existencias suficientes
     * (RN-01); como el producto está bloqueado, dos salidas simultáneas nunca dejan saldo negativo.
     *
     * @param producto      producto bloqueado
     * @param cantidad      cantidad validada
     * @param tipo          tipo de movimiento de naturaleza SALIDA
     * @param documentoTipo documento origen
     * @param documentoId   id del documento origen
     * @return movimiento guardado (su costo unitario es el CPP con que salió)
     * @throws NegocioException {@code EXISTENCIAS_INSUFICIENTES}
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public MovimientoInventario registrarSalida(Producto producto, BigDecimal cantidad, TipoMovimiento tipo,
                                                TipoDocumentoComercial documentoTipo, Long documentoId) {
        exigirNaturaleza(tipo, Naturaleza.SALIDA);
        BigDecimal saldo = producto.getExistencias().subtract(cantidad);
        if (saldo.signum() < 0) {
            throw new NegocioException(CodigoError.EXISTENCIAS_INSUFICIENTES, producto.getCodigo(),
                producto.getExistencias().stripTrailingZeros().toPlainString());
        }
        BigDecimal costo = producto.getCostoPromedio();
        return aplicar(producto, tipo, cantidad, costo, Montos.cantidad(saldo), costo, costo, documentoTipo, documentoId);
    }

    private MovimientoInventario aplicar(Producto producto, TipoMovimiento tipo, BigDecimal cantidad,
                                         BigDecimal costoUnitario, BigDecimal saldo, BigDecimal costoAnterior,
                                         BigDecimal costoNuevo, TipoDocumentoComercial documentoTipo, Long documentoId) {
        producto.setExistencias(saldo);
        producto.setCostoPromedio(costoNuevo);
        return movimientos.save(MovimientoInventario.builder()
            .productoId(producto.getId())
            .tipo(tipo)
            .naturaleza(tipo.getNaturaleza())
            .cantidad(cantidad)
            .costoUnitario(costoUnitario)
            .saldoResultante(saldo)
            .costoPromedioAnterior(costoAnterior)
            .costoPromedioResultante(costoNuevo)
            .documentoTipo(documentoTipo)
            .documentoId(documentoId)
            .usuarioId(UsuarioActual.obtener().map(UsuarioAutenticado::getId)
                .orElseThrow(() -> new NegocioException(CodigoError.NO_AUTENTICADO)))
            .fecha(reloj.instant())
            .build());
    }

    private static void exigirNaturaleza(TipoMovimiento tipo, Naturaleza esperada) {
        if (tipo.getNaturaleza() != esperada) {
            throw new IllegalArgumentException("El movimiento " + tipo + " no es de " + esperada);
        }
    }
}
