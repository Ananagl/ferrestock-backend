/*
 * Nombre:                 InventarioConsultaServicio.java
 * Descripción:            Consultas de inventario: existencias, kardex, valorización y verificación de consistencia.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.6.0
 */
package com.ferrestock.inventario.servicio;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ferrestock.catalogo.entidad.Producto;
import com.ferrestock.catalogo.servicio.FiltroProductos;
import com.ferrestock.catalogo.servicio.ProductoServicio;
import com.ferrestock.compartido.numeros.Montos;
import com.ferrestock.compartido.respuesta.PaginaResponse;
import com.ferrestock.compartido.seguridad.UsuarioActual;
import com.ferrestock.compartido.seguridad.UsuarioAutenticado;
import com.ferrestock.inventario.dto.respuesta.ExistenciaResponse;
import com.ferrestock.inventario.dto.respuesta.InconsistenciaResponse;
import com.ferrestock.inventario.dto.respuesta.MovimientoResponse;
import com.ferrestock.inventario.dto.respuesta.ValorizacionResponse;
import com.ferrestock.inventario.entidad.MovimientoInventario;
import com.ferrestock.inventario.repositorio.MovimientoInventarioRepositorio;
import com.ferrestock.usuarios.servicio.UsuarioServicio;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consultas del módulo de inventario. Los costos solo se incluyen si el usuario es ADMIN (DP-15).
 */
@Service
@RequiredArgsConstructor
public class InventarioConsultaServicio {

    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Bogota");

    private final ProductoServicio productos;
    private final MovimientoInventarioRepositorio movimientos;
    private final UsuarioServicio usuarios;

    /**
     * Existencias por producto (RF-INV-001).
     *
     * @param filtro             filtros del catálogo
     * @param soloConExistencias si solo se incluyen productos con existencias
     * @param pagina             número de página
     * @param tamano             tamaño (máximo 100)
     * @return página de existencias
     */
    @Transactional(readOnly = true)
    public PaginaResponse<ExistenciaResponse> existencias(FiltroProductos filtro, boolean soloConExistencias, int pagina,
                                                          int tamano) {
        boolean admin = esAdmin();
        Page<Producto> resultado = productos.buscarParaInventario(filtro, soloConExistencias,
            PaginaResponse.solicitud(pagina, tamano, Sort.by("nombre").and(Sort.by("id"))));
        return PaginaResponse.de(resultado.map(p -> new ExistenciaResponse(p.getId(), p.getCodigo(), p.getNombre(),
            p.getCategoria().getNombre(), p.getUnidadMedida().name(), p.getEstado().name(), p.getExistencias(),
            admin ? p.getCostoPromedio() : null,
            admin ? Montos.dinero(p.getExistencias().multiply(p.getCostoPromedio())) : null)));
    }

    /**
     * Kardex de un producto, del movimiento más reciente al más antiguo (RF-INV-003).
     *
     * @param productoId producto
     * @param filtro     filtros por fecha y tipo
     * @param pagina     número de página
     * @param tamano     tamaño (máximo 100)
     * @return página de movimientos
     */
    @Transactional(readOnly = true)
    public PaginaResponse<MovimientoResponse> kardex(Long productoId, FiltroKardex filtro, int pagina, int tamano) {
        productos.entidad(productoId);
        boolean admin = esAdmin();
        Page<MovimientoInventario> resultado = movimientos.findAll(especificacion(productoId, filtro),
            PaginaResponse.solicitud(pagina, tamano, Sort.by(Sort.Order.desc("id"))));
        Map<Long, String> nombres = usuarios.nombresPorId(resultado.map(MovimientoInventario::getUsuarioId).toSet());
        return PaginaResponse.de(resultado.map(m -> new MovimientoResponse(m.getId(), m.getFecha(), m.getTipo().name(),
            m.getNaturaleza().name(), m.getCantidad(), m.getSaldoResultante(), m.getDocumentoTipo().name(),
            m.getDocumentoId(), nombres.get(m.getUsuarioId()), admin ? m.getCostoUnitario() : null,
            admin ? m.getCostoPromedioResultante() : null)));
    }

    /**
     * Valor del inventario total y por categoría (RF-COS-002, solo ADMIN).
     *
     * @return valorización
     */
    @Transactional(readOnly = true)
    public ValorizacionResponse valorizacion() {
        List<ValorizacionResponse.Categoria> categorias = productos.valorizarPorCategoria().stream()
            .map(v -> new ValorizacionResponse.Categoria(v.getCategoriaId(), v.getCategoria(), v.getProductos(),
                Montos.dinero(v.getValor())))
            .toList();
        BigDecimal total = categorias.stream().map(ValorizacionResponse.Categoria::valor)
            .reduce(Montos.dinero(BigDecimal.ZERO), BigDecimal::add);
        return new ValorizacionResponse(total, categorias);
    }

    /**
     * Compara cada producto con su kardex (03_inventario_y_costos.md, sección 8).
     *
     * @return productos inconsistentes; vacío si todo coincide
     */
    @Transactional(readOnly = true)
    public List<InconsistenciaResponse> verificar() {
        return movimientos.buscarInconsistencias().stream()
            .map(i -> new InconsistenciaResponse(i.getProductoId(), i.getCodigo(), i.getExistencias(),
                i.getExistenciasKardex(), i.getCostoPromedio(), i.getCostoPromedioKardex()))
            .toList();
    }

    private static boolean esAdmin() {
        return UsuarioActual.obtener().map(UsuarioAutenticado::esAdmin).orElse(false);
    }

    private static Specification<MovimientoInventario> especificacion(Long productoId, FiltroKardex filtro) {
        return (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(cb.equal(raiz.get("productoId"), productoId));
            if (filtro.tipo() != null) {
                condiciones.add(cb.equal(raiz.get("tipo"), filtro.tipo()));
            }
            if (filtro.desde() != null) {
                condiciones.add(cb.greaterThanOrEqualTo(raiz.get("fecha"), filtro.desde().atStartOfDay(ZONA_NEGOCIO).toInstant()));
            }
            if (filtro.hasta() != null) {
                condiciones.add(cb.lessThan(raiz.get("fecha"), filtro.hasta().plusDays(1).atStartOfDay(ZONA_NEGOCIO).toInstant()));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }
}
