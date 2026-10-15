/*
 * Nombre:                 ProductoServicio.java
 * Descripción:            Casos de uso de productos y precios (RF-PRO-001 a 007, RF-PRE-001 y 002, RF-COS-002).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.4.0
 */
package com.ferrestock.catalogo.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.ferrestock.auditoria.entidad.AccionAuditoria;
import com.ferrestock.auditoria.servicio.AuditoriaServicio;
import com.ferrestock.catalogo.dto.respuesta.CostoProductoResponse;
import com.ferrestock.catalogo.dto.respuesta.ProductoResponse;
import com.ferrestock.catalogo.dto.solicitud.ProductoActualizarRequest;
import com.ferrestock.catalogo.dto.solicitud.ProductoCrearRequest;
import com.ferrestock.catalogo.entidad.EstadoProducto;
import com.ferrestock.catalogo.entidad.Producto;
import com.ferrestock.catalogo.repositorio.ProductoRepositorio;
import com.ferrestock.compartido.error.CodigoError;
import com.ferrestock.compartido.error.NegocioException;
import com.ferrestock.compartido.respuesta.OrdenSeguro;
import com.ferrestock.compartido.respuesta.PaginaResponse;
import com.ferrestock.compartido.seguridad.UsuarioActual;
import com.ferrestock.compartido.seguridad.UsuarioAutenticado;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Catálogo de productos. Reglas:
 * <ul>
 *   <li>Código interno único sin distinguir mayúsculas; se guarda en mayúsculas (RNP-01).</li>
 *   <li>Solo se asignan categorías activas.</li>
 *   <li>La edición usa bloqueo optimista: si la versión enviada no es la vigente, responde
 *       {@code VERSION_DESACTUALIZADA} (ADR-08).</li>
 *   <li>No existe eliminación: los productos se desactivan y conservan su historial (RN-02).</li>
 *   <li>El precio se cambia aparte y siempre se audita (RN-09); las ventas pasadas conservan su precio.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ProductoServicio {

    private static final Set<String> CAMPOS_ORDEN = Set.of("codigo", "nombre", "precioVenta", "existencias", "creadoEn");

    private final ProductoRepositorio productos;
    private final CategoriaServicio categorias;
    private final AuditoriaServicio auditoria;

    /**
     * @param filtro filtros opcionales
     * @param pagina número de página
     * @param tamano tamaño (máximo 100)
     * @param orden  campo,dirección de la lista blanca
     * @return página de productos
     */
    @Transactional(readOnly = true)
    public PaginaResponse<ProductoResponse> listar(FiltroProductos filtro, int pagina, int tamano, String orden) {
        Sort ordenamiento = OrdenSeguro.de(orden, CAMPOS_ORDEN, Sort.by("nombre")).and(Sort.by("id"));
        return PaginaResponse.de(productos.findAll(especificacion(filtro),
            PaginaResponse.solicitud(pagina, tamano, ordenamiento)).map(ProductoResponse::desde));
    }

    /**
     * @param id producto
     * @return el producto
     */
    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return ProductoResponse.desde(buscar(id));
    }

    /**
     * Registra un producto ACTIVO con existencias y costo en cero (RF-PRO-001, RF-PRO-002).
     *
     * @param solicitud datos del producto
     * @return producto creado
     */
    @Transactional
    public ProductoResponse crear(ProductoCrearRequest solicitud) {
        String codigo = solicitud.codigo().trim().toUpperCase(Locale.ROOT);
        if (productos.findByCodigoIgnoreCase(codigo).isPresent()) {
            throw new NegocioException(CodigoError.DUPLICADO, "codigo");
        }
        Long actor = actorId();
        Producto producto = productos.saveAndFlush(Producto.builder()
            .codigo(codigo)
            .nombre(solicitud.nombre().trim())
            .descripcion(textoOpcional(solicitud.descripcion()))
            .categoria(categorias.activaParaAsignar(solicitud.categoriaId()))
            .unidadMedida(solicitud.unidadMedida())
            .permiteFraccion(solicitud.permiteFraccion())
            .precioVenta(solicitud.precioVenta().setScale(2, RoundingMode.HALF_UP))
            .creadoPorId(actor)
            .actualizadoPorId(actor)
            .build());
        auditoria.registrar(AccionAuditoria.PRODUCTO_CREADO,
            "Alta del producto " + codigo + " (" + producto.getNombre() + ") con precio " + producto.getPrecioVenta());
        return ProductoResponse.desde(producto);
    }

    /**
     * Modifica los datos descriptivos (RF-PRO-005). Existencias, costo y precio no cambian aquí.
     *
     * @param id        producto
     * @param solicitud nuevos datos y versión leída
     * @return producto actualizado
     */
    @Transactional
    public ProductoResponse actualizar(Long id, ProductoActualizarRequest solicitud) {
        Producto producto = buscar(id);
        exigirVersion(producto, solicitud.version());
        if (!producto.getCategoria().getId().equals(solicitud.categoriaId())) {
            producto.setCategoria(categorias.activaParaAsignar(solicitud.categoriaId()));
        }
        producto.setNombre(solicitud.nombre().trim());
        producto.setDescripcion(textoOpcional(solicitud.descripcion()));
        producto.setUnidadMedida(solicitud.unidadMedida());
        producto.setPermiteFraccion(solicitud.permiteFraccion());
        producto.setActualizadoPorId(actorId());
        Producto guardado = productos.saveAndFlush(producto);
        auditoria.registrar(AccionAuditoria.PRODUCTO_ACTUALIZADO, "Datos del producto " + producto.getCodigo()
            + " actualizados");
        return ProductoResponse.desde(guardado);
    }

    /**
     * Activa o desactiva el producto (RF-PRO-006, RF-PRO-007). Nunca hay eliminación física.
     *
     * @param id     producto
     * @param estado nuevo estado
     * @return producto actualizado
     */
    @Transactional
    public ProductoResponse cambiarEstado(Long id, EstadoProducto estado) {
        Producto producto = buscar(id);
        if (producto.getEstado() != estado) {
            producto.setEstado(estado);
            producto.setActualizadoPorId(actorId());
            producto = productos.saveAndFlush(producto);
            auditoria.registrar(estado == EstadoProducto.ACTIVO ? AccionAuditoria.PRODUCTO_ACTIVADO
                : AccionAuditoria.PRODUCTO_DESACTIVADO, (estado == EstadoProducto.ACTIVO ? "Reactivación"
                : "Desactivación") + " del producto " + producto.getCodigo());
        }
        return ProductoResponse.desde(producto);
    }

    /**
     * Cambia el precio de venta y lo audita (RF-PRE-001, RF-PRE-002). Solo afecta ventas posteriores, porque cada
     * venta guarda su propio precio unitario.
     *
     * @param id          producto
     * @param precioVenta nuevo precio, mayor que cero
     * @return producto actualizado
     */
    @Transactional
    public ProductoResponse cambiarPrecio(Long id, BigDecimal precioVenta) {
        Producto producto = buscar(id);
        BigDecimal nuevo = precioVenta.setScale(2, RoundingMode.HALF_UP);
        BigDecimal anterior = producto.getPrecioVenta();
        if (anterior.compareTo(nuevo) != 0) {
            producto.setPrecioVenta(nuevo);
            producto.setActualizadoPorId(actorId());
            producto = productos.saveAndFlush(producto);
            auditoria.registrar(AccionAuditoria.PRECIO_MODIFICADO, "Cambio de precio de " + producto.getCodigo()
                + " de " + anterior.toPlainString() + " a " + nuevo.toPlainString());
        }
        return ProductoResponse.desde(producto);
    }

    /**
     * Costo promedio y valor del inventario de un producto (RF-COS-002; solo ADMIN, DP-15).
     *
     * @param id producto
     * @return costo y valorización
     */
    @Transactional(readOnly = true)
    public CostoProductoResponse costo(Long id) {
        Producto producto = buscar(id);
        BigDecimal valor = producto.getExistencias().multiply(producto.getCostoPromedio())
            .setScale(2, RoundingMode.HALF_UP);
        return new CostoProductoResponse(producto.getId(), producto.getCostoPromedio(), producto.getExistencias(), valor);
    }

    private Producto buscar(Long id) {
        return productos.findById(id).orElseThrow(() -> new NegocioException(CodigoError.NO_ENCONTRADO));
    }

    private static void exigirVersion(Producto producto, Long versionCliente) {
        if (!producto.getVersion().equals(versionCliente)) {
            throw new NegocioException(CodigoError.VERSION_DESACTUALIZADA);
        }
    }

    private static String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static Long actorId() {
        return UsuarioActual.obtener().map(UsuarioAutenticado::getId)
            .orElseThrow(() -> new NegocioException(CodigoError.NO_AUTENTICADO));
    }

    private static Specification<Producto> especificacion(FiltroProductos filtro) {
        return (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (filtro.categoriaId() != null) {
                condiciones.add(cb.equal(raiz.get("categoria").get("id"), filtro.categoriaId()));
            }
            if (filtro.estado() != null) {
                condiciones.add(cb.equal(raiz.get("estado"), filtro.estado()));
            }
            if (filtro.texto() != null && !filtro.texto().isBlank()) {
                String patron = "%" + filtro.texto().trim().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                condiciones.add(cb.or(
                    cb.like(cb.lower(raiz.get("codigo")), patron, '\\'),
                    cb.like(cb.lower(raiz.get("nombre")), patron, '\\')));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }
}
