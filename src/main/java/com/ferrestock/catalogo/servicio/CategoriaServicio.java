/*
 * Nombre:                 CategoriaServicio.java
 * Descripción:            Casos de uso de categorías (RF-PRO-009): listar, crear, renombrar y cambiar estado.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.4.0
 */
package com.ferrestock.catalogo.servicio;

import java.util.List;

import com.ferrestock.auditoria.entidad.AccionAuditoria;
import com.ferrestock.auditoria.servicio.AuditoriaServicio;
import com.ferrestock.catalogo.dto.respuesta.CategoriaResponse;
import com.ferrestock.catalogo.entidad.Categoria;
import com.ferrestock.catalogo.repositorio.CategoriaRepositorio;
import com.ferrestock.compartido.error.CodigoError;
import com.ferrestock.compartido.error.NegocioException;
import com.ferrestock.compartido.seguridad.UsuarioActual;
import com.ferrestock.compartido.seguridad.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestión de categorías. El nombre es único sin distinguir mayúsculas; una categoría inactiva sigue existiendo
 * (sus productos la conservan) pero no se asigna a productos nuevos ni editados.
 */
@Service
@RequiredArgsConstructor
public class CategoriaServicio {

    private final CategoriaRepositorio categorias;
    private final AuditoriaServicio auditoria;

    /**
     * @param activa filtro de estado, o {@code null} para todas
     * @return categorías ordenadas por nombre
     */
    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar(Boolean activa) {
        List<Categoria> resultado = activa == null ? categorias.findAllByOrderByNombreAsc()
            : categorias.findByActivaOrderByNombreAsc(activa);
        return resultado.stream().map(CategoriaResponse::desde).toList();
    }

    /**
     * @param nombre nombre de la categoría
     * @return categoría creada
     * @throws NegocioException {@code DUPLICADO} en el campo {@code nombre}
     */
    @Transactional
    public CategoriaResponse crear(String nombre) {
        String limpio = nombre.trim();
        exigirNombreLibre(limpio, null);
        Long actor = actorId();
        Categoria categoria = categorias.save(Categoria.builder().nombre(limpio).creadoPorId(actor)
            .actualizadoPorId(actor).build());
        auditoria.registrar(AccionAuditoria.CATEGORIA_CREADA, "Alta de la categoría " + limpio);
        return CategoriaResponse.desde(categoria);
    }

    /**
     * @param id     categoría
     * @param nombre nuevo nombre
     * @return categoría actualizada
     */
    @Transactional
    public CategoriaResponse renombrar(Long id, String nombre) {
        Categoria categoria = buscar(id);
        String limpio = nombre.trim();
        exigirNombreLibre(limpio, id);
        String anterior = categoria.getNombre();
        categoria.setNombre(limpio);
        categoria.setActualizadoPorId(actorId());
        auditoria.registrar(AccionAuditoria.CATEGORIA_ACTUALIZADA,
            "Categoría " + anterior + " renombrada a " + limpio);
        return CategoriaResponse.desde(categorias.saveAndFlush(categoria));
    }

    /**
     * @param id     categoría
     * @param activa nuevo estado
     * @return categoría actualizada
     */
    @Transactional
    public CategoriaResponse cambiarEstado(Long id, boolean activa) {
        Categoria categoria = buscar(id);
        if (categoria.isActiva() != activa) {
            categoria.setActiva(activa);
            categoria.setActualizadoPorId(actorId());
            auditoria.registrar(AccionAuditoria.CATEGORIA_ESTADO_CAMBIADO,
                "Categoría " + categoria.getNombre() + (activa ? " activada" : " desactivada"));
        }
        return CategoriaResponse.desde(categorias.saveAndFlush(categoria));
    }

    /**
     * Devuelve una categoría activa para asignarla a un producto.
     *
     * @param id categoría
     * @return la entidad
     * @throws NegocioException {@code NO_ENCONTRADO} o {@code CATEGORIA_INACTIVA}, en el campo {@code categoriaId}
     */
    @Transactional(readOnly = true)
    public Categoria activaParaAsignar(Long id) {
        Categoria categoria = categorias.findById(id)
            .orElseThrow(() -> new NegocioException(CodigoError.NO_ENCONTRADO, "categoriaId"));
        if (!categoria.isActiva()) {
            throw new NegocioException(CodigoError.CATEGORIA_INACTIVA, "categoriaId");
        }
        return categoria;
    }

    private Categoria buscar(Long id) {
        return categorias.findById(id).orElseThrow(() -> new NegocioException(CodigoError.NO_ENCONTRADO));
    }

    private void exigirNombreLibre(String nombre, Long idPropio) {
        categorias.findByNombreIgnoreCase(nombre)
            .filter(existente -> !existente.getId().equals(idPropio))
            .ifPresent(existente -> {
                throw new NegocioException(CodigoError.DUPLICADO, "nombre");
            });
    }

    private static Long actorId() {
        return UsuarioActual.obtener().map(UsuarioAutenticado::getId)
            .orElseThrow(() -> new NegocioException(CodigoError.NO_AUTENTICADO));
    }
}
