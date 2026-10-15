/*
 * Nombre:                 ComprobanteServicio.java
 * Descripción:            Búsqueda y armado de comprobantes de compras, ventas, devoluciones y ajustes a partir de los documentos guardados.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.10.0
 */
package com.ferrestock.comprobantes.servicio;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.ferrestock.compartido.documentos.LineasDocumento;
import com.ferrestock.compartido.documentos.TipoDocumentoComercial;
import com.ferrestock.compartido.error.CodigoError;
import com.ferrestock.compartido.error.NegocioException;
import com.ferrestock.compartido.respuesta.PaginaResponse;
import com.ferrestock.compartido.seguridad.UsuarioActual;
import com.ferrestock.compartido.seguridad.UsuarioAutenticado;
import com.ferrestock.comprobantes.dto.respuesta.ComprobanteResponse;
import com.ferrestock.comprobantes.dto.respuesta.ComprobanteResumen;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comprobantes (CU-08). No se guardan aparte: se arman desde el documento, así nunca difieren de él (ADR-18, sin PDF
 * en el backend). Los comprobantes de ajuste solo los ve el ADMIN (DP-16).
 */
@Service
@RequiredArgsConstructor
public class ComprobanteServicio {

    private static final String DOCUMENTOS = """
        SELECT 'COMPRA' AS tipo, c.consecutivo, c.fecha, t.nombre AS tercero, c.total, c.estado
          FROM compra c JOIN tercero t ON t.id = c.proveedor_id
        UNION ALL
        SELECT 'VENTA', v.consecutivo, v.fecha, t.nombre, v.total, v.estado
          FROM venta v JOIN tercero t ON t.id = v.cliente_id
        UNION ALL
        SELECT 'DEVOLUCION', d.consecutivo, d.fecha, t.nombre, NULL, 'REGISTRADA'
          FROM devolucion d JOIN tercero t ON t.id = d.cliente_id
        UNION ALL
        SELECT 'AJUSTE', a.consecutivo, a.fecha, p.codigo || ' — ' || p.nombre, NULL, 'REGISTRADA'
          FROM ajuste a JOIN producto p ON p.id = a.producto_id""";

    private final NamedParameterJdbcTemplate jdbc;
    private final MessageSource mensajes;

    @Value("${ferrestock.negocio.nombre:Ferretería FerreStock}")
    private String nombreNegocio;
    @Value("${ferrestock.negocio.nit:900000000-0}")
    private String nitNegocio;
    @Value("${ferrestock.negocio.direccion:Colombia}")
    private String direccionNegocio;

    /**
     * Búsqueda unificada por tipo, consecutivo (parcial) y fechas (RF-DOC-005).
     *
     * @param tipo        tipo de documento o nulo
     * @param consecutivo texto del consecutivo o nulo
     * @param desde       fecha inicial o nula
     * @param hasta       fecha final o nula
     * @param pagina      número de página
     * @param tamano      tamaño (máximo 100)
     * @return comprobantes del más reciente al más antiguo
     */
    @Transactional(readOnly = true)
    public PaginaResponse<ComprobanteResumen> buscar(TipoDocumentoComercial tipo, String consecutivo, LocalDate desde,
                                                      LocalDate hasta, int pagina, int tamano) {
        PageRequest solicitud = PaginaResponse.solicitud(pagina, tamano, Sort.unsorted());
        List<String> condiciones = new ArrayList<>();
        MapSqlParameterSource parametros = new MapSqlParameterSource();
        if (!esAdmin()) {
            condiciones.add("tipo <> 'AJUSTE'");
        }
        if (tipo != null) {
            condiciones.add("tipo = :tipo");
            parametros.addValue("tipo", tipo.name());
        }
        if (consecutivo != null && !consecutivo.isBlank()) {
            condiciones.add("consecutivo ILIKE :consecutivo");
            parametros.addValue("consecutivo", "%" + consecutivo.trim() + "%");
        }
        if (desde != null) {
            condiciones.add("fecha >= :desde");
            parametros.addValue("desde", Timestamp.from(desde.atStartOfDay(LineasDocumento.ZONA_NEGOCIO).toInstant()));
        }
        if (hasta != null) {
            condiciones.add("fecha < :hasta");
            parametros.addValue("hasta", Timestamp.from(hasta.plusDays(1).atStartOfDay(LineasDocumento.ZONA_NEGOCIO).toInstant()));
        }
        String filtro = "FROM (" + DOCUMENTOS + ") x" + (condiciones.isEmpty() ? "" : " WHERE " + String.join(" AND ", condiciones));
        Long total = jdbc.queryForObject("SELECT count(*) " + filtro, parametros, Long.class);
        parametros.addValue("limite", solicitud.getPageSize()).addValue("desplazamiento", solicitud.getOffset());
        List<ComprobanteResumen> contenido = jdbc.query("SELECT * " + filtro
            + " ORDER BY fecha DESC, consecutivo DESC LIMIT :limite OFFSET :desplazamiento", parametros,
            (rs, i) -> new ComprobanteResumen(rs.getString("tipo"), rs.getString("consecutivo"),
                rs.getTimestamp("fecha").toInstant(), rs.getString("tercero"), rs.getBigDecimal("total"), rs.getString("estado")));
        return PaginaResponse.de(new PageImpl<>(contenido, solicitud, total == null ? 0 : total));
    }

    /**
     * Arma el comprobante de un documento (RF-DOC-002 a 004).
     *
     * @param tipo        tipo de documento
     * @param consecutivo consecutivo exacto
     * @param idioma      idioma de la leyenda
     * @return comprobante
     * @throws NegocioException {@code NO_ENCONTRADO} o {@code ACCESO_DENEGADO} (ajustes para no ADMIN)
     */
    @Transactional(readOnly = true)
    public ComprobanteResponse obtener(TipoDocumentoComercial tipo, String consecutivo, Locale idioma) {
        if (tipo == TipoDocumentoComercial.AJUSTE && !esAdmin()) {
            throw new NegocioException(CodigoError.ACCESO_DENEGADO);
        }
        MapSqlParameterSource p = new MapSqlParameterSource("consecutivo", consecutivo);
        try {
            return switch (tipo) {
                case VENTA -> venta(p, idioma);
                case COMPRA -> compra(p, idioma);
                case DEVOLUCION -> devolucion(p, idioma);
                case AJUSTE -> ajuste(p, idioma);
            };
        } catch (EmptyResultDataAccessException e) {
            throw new NegocioException(CodigoError.NO_ENCONTRADO);
        }
    }

    private ComprobanteResponse venta(MapSqlParameterSource p, Locale idioma) {
        return jdbc.queryForObject("""
            SELECT v.id, v.consecutivo, v.fecha, v.estado, v.total, v.valor_recibido, v.cambio, v.motivo_anulacion,
                   t.nombre, t.tipo_documento, t.numero_documento, t.digito_verificacion, u.nombre_usuario
              FROM venta v JOIN tercero t ON t.id = v.cliente_id JOIN usuario u ON u.id = v.creado_por_id
             WHERE v.consecutivo = :consecutivo""", p, (rs, i) -> new ComprobanteResponse(negocio(), "VENTA",
            rs.getString("consecutivo"), rs.getTimestamp("fecha").toInstant(), rs.getString("estado"), tercero(rs),
            rs.getString("nombre_usuario"), null, rs.getString("motivo_anulacion"), lineas("""
                SELECT p.codigo, p.nombre, l.cantidad, l.precio_unitario AS valor, l.subtotal, NULL AS detalle
                  FROM venta_linea l JOIN producto p ON p.id = l.producto_id WHERE l.venta_id = :id ORDER BY l.id""",
                rs.getLong("id")), rs.getBigDecimal("total"), rs.getBigDecimal("valor_recibido"), rs.getBigDecimal("cambio"),
            leyenda(idioma)));
    }

    private ComprobanteResponse compra(MapSqlParameterSource p, Locale idioma) {
        return jdbc.queryForObject("""
            SELECT c.id, c.consecutivo, c.fecha, c.estado, c.total, c.observacion, c.motivo_anulacion,
                   t.nombre, t.tipo_documento, t.numero_documento, t.digito_verificacion, u.nombre_usuario
              FROM compra c JOIN tercero t ON t.id = c.proveedor_id JOIN usuario u ON u.id = c.creado_por_id
             WHERE c.consecutivo = :consecutivo""", p, (rs, i) -> new ComprobanteResponse(negocio(), "COMPRA",
            rs.getString("consecutivo"), rs.getTimestamp("fecha").toInstant(), rs.getString("estado"), tercero(rs),
            rs.getString("nombre_usuario"), rs.getString("observacion"), rs.getString("motivo_anulacion"), lineas("""
                SELECT p.codigo, p.nombre, l.cantidad, l.costo_unitario AS valor, l.subtotal, NULL AS detalle
                  FROM compra_linea l JOIN producto p ON p.id = l.producto_id WHERE l.compra_id = :id ORDER BY l.id""",
                rs.getLong("id")), rs.getBigDecimal("total"), null, null, leyenda(idioma)));
    }

    private ComprobanteResponse devolucion(MapSqlParameterSource p, Locale idioma) {
        return jdbc.queryForObject("""
            SELECT d.id, d.consecutivo, d.fecha, d.motivo, v.consecutivo AS venta,
                   t.nombre, t.tipo_documento, t.numero_documento, t.digito_verificacion, u.nombre_usuario
              FROM devolucion d JOIN tercero t ON t.id = d.cliente_id JOIN usuario u ON u.id = d.validada_por_id
              LEFT JOIN venta v ON v.id = d.venta_id
             WHERE d.consecutivo = :consecutivo""", p, (rs, i) -> new ComprobanteResponse(negocio(), "DEVOLUCION",
            rs.getString("consecutivo"), rs.getTimestamp("fecha").toInstant(), "REGISTRADA", tercero(rs),
            rs.getString("nombre_usuario"), rs.getString("venta"), rs.getString("motivo"), lineas("""
                SELECT p.codigo, p.nombre, l.cantidad, NULL AS valor, NULL AS subtotal, l.resultado AS detalle
                  FROM devolucion_linea l JOIN producto p ON p.id = l.producto_id WHERE l.devolucion_id = :id ORDER BY l.id""",
                rs.getLong("id")), null, null, null, leyenda(idioma)));
    }

    private ComprobanteResponse ajuste(MapSqlParameterSource p, Locale idioma) {
        return jdbc.queryForObject("""
            SELECT a.consecutivo, a.fecha, a.motivo, a.tipo, a.cantidad, a.costo_unitario, p.codigo, p.nombre, u.nombre_usuario
              FROM ajuste a JOIN producto p ON p.id = a.producto_id JOIN usuario u ON u.id = a.creado_por_id
             WHERE a.consecutivo = :consecutivo""", p, (rs, i) -> new ComprobanteResponse(negocio(), "AJUSTE",
            rs.getString("consecutivo"), rs.getTimestamp("fecha").toInstant(), "REGISTRADA", null,
            rs.getString("nombre_usuario"), null, rs.getString("motivo"), List.of(new ComprobanteResponse.Linea(
                rs.getString("codigo"), rs.getString("nombre"), rs.getBigDecimal("cantidad"), rs.getBigDecimal("costo_unitario"),
                null, rs.getString("tipo"))), null, null, null, leyenda(idioma)));
    }

    private List<ComprobanteResponse.Linea> lineas(String sql, long id) {
        return jdbc.query(sql, new MapSqlParameterSource("id", id), (rs, i) -> new ComprobanteResponse.Linea(
            rs.getString("codigo"), rs.getString("nombre"), rs.getBigDecimal("cantidad"), (BigDecimal) rs.getObject("valor"),
            rs.getBigDecimal("subtotal"), rs.getString("detalle")));
    }

    private static ComprobanteResponse.Tercero tercero(ResultSet rs) throws SQLException {
        String documento = rs.getString("tipo_documento") + " " + rs.getString("numero_documento")
            + (rs.getString("digito_verificacion") == null ? "" : "-" + rs.getString("digito_verificacion"));
        return new ComprobanteResponse.Tercero(rs.getString("nombre"), documento);
    }

    private ComprobanteResponse.Negocio negocio() {
        return new ComprobanteResponse.Negocio(nombreNegocio, nitNegocio, direccionNegocio);
    }

    private String leyenda(Locale idioma) {
        return mensajes.getMessage("comprobante.leyenda", null, idioma);
    }

    private static boolean esAdmin() {
        return UsuarioActual.obtener().map(UsuarioAutenticado::esAdmin).orElse(false);
    }
}
