/*
 * Nombre:                 PaginaResponse.java
 * Descripción:            Respuesta paginada estándar de la API y utilidades para crear solicitudes de página con tamaño acotado.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.2.0
 */
package com.ferrestock.compartido.respuesta;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Página de resultados (04_api_rest.md, sección 2).
 *
 * @param contenido      elementos de la página
 * @param pagina         número de página, desde 0
 * @param tamano         tamaño de página aplicado
 * @param totalElementos total de elementos que cumplen el filtro
 * @param totalPaginas   total de páginas
 * @param <T>            tipo de los elementos
 */
public record PaginaResponse<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

    /** Tamaño de página por defecto. */
    public static final int TAMANO_POR_DEFECTO = 20;

    /** Tamaño máximo permitido (RNF-DES-02). */
    public static final int TAMANO_MAXIMO = 100;

    /**
     * Convierte una página de Spring Data.
     *
     * @param pagina página de Spring Data
     * @param <T>    tipo de los elementos
     * @return respuesta paginada
     */
    public static <T> PaginaResponse<T> de(Page<T> pagina) {
        return new PaginaResponse<>(pagina.getContent(), pagina.getNumber(), pagina.getSize(),
            pagina.getTotalElements(), pagina.getTotalPages());
    }

    /**
     * Crea una solicitud de página corrigiendo valores fuera de rango: página negativa pasa a 0 y el tamaño se
     * limita a {@value #TAMANO_MAXIMO}.
     *
     * @param pagina número de página solicitado
     * @param tamano tamaño solicitado
     * @param orden  ordenamiento (debe venir de una lista blanca del servicio)
     * @return solicitud de página segura
     */
    public static PageRequest solicitud(int pagina, int tamano, Sort orden) {
        int tamanoSeguro = tamano < 1 ? TAMANO_POR_DEFECTO : Math.min(tamano, TAMANO_MAXIMO);
        return PageRequest.of(Math.max(pagina, 0), tamanoSeguro, orden);
    }
}
