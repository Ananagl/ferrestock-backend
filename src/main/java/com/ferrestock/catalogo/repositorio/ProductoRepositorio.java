/*
 * Nombre:                 ProductoRepositorio.java
 * Descripción:            Acceso a datos de productos con filtros dinámicos.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.6.0
 */
package com.ferrestock.catalogo.repositorio;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.ferrestock.catalogo.entidad.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de {@link Producto}.
 */
public interface ProductoRepositorio extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

    /**
     * @param codigo código interno, sin distinguir mayúsculas
     * @return el producto, si existe
     */
    Optional<Producto> findByCodigoIgnoreCase(String codigo);

    /**
     * Listado filtrado que trae la categoría en la misma consulta (evita N+1 consultas).
     *
     * @param especificacion filtros
     * @param pagina         página solicitada
     * @return página de productos
     */
    @Override
    @EntityGraph(attributePaths = "categoria")
    Page<Producto> findAll(Specification<Producto> especificacion, Pageable pagina);

    /**
     * Bloquea los productos para modificar sus existencias ({@code SELECT … FOR UPDATE}), siempre en orden de id
     * para que dos operaciones concurrentes no se bloqueen mutuamente (ADR-08).
     *
     * @param ids productos a bloquear
     * @return productos bloqueados, ordenados por id
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id in :ids order by p.id")
    List<Producto> bloquearPorIds(@Param("ids") Collection<Long> ids);

    /**
     * Valor del inventario por categoría (RF-COS-002).
     */
    interface ValorPorCategoria {
        /** @return id de la categoría */
        Long getCategoriaId();

        /** @return nombre de la categoría */
        String getCategoria();

        /** @return productos con existencias */
        Long getProductos();

        /** @return suma de existencias × costo promedio */
        BigDecimal getValor();
    }

    /**
     * @return valor del inventario agrupado por categoría (solo productos con existencias)
     */
    @Query("""
        select c.id as categoriaId, c.nombre as categoria, count(p) as productos,
               sum(p.existencias * p.costoPromedio) as valor
        from Producto p join p.categoria c
        where p.existencias > 0
        group by c.id, c.nombre
        order by c.nombre""")
    List<ValorPorCategoria> valorizarPorCategoria();
}
