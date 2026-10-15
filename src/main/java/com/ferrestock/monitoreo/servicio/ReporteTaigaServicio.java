/*
 * Nombre:                 ReporteTaigaServicio.java
 * Descripción:            Crea en Taiga un issue de tipo error de producción cuando un error se vuelve recurrente (RF-MON-006, ADR-17).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.13.0
 */
package com.ferrestock.monitoreo.servicio;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente mínimo de la API de Taiga. Si no hay credenciales configuradas ({@code ferrestock.taiga.*}), no hace nada:
 * el error igual queda en {@code registro_error}. Un fallo de Taiga nunca afecta la petición del usuario.
 */
@Slf4j
@Service
public class ReporteTaigaServicio {

    private final RestClient cliente;
    private final String usuario;
    private final String clave;
    private final Long proyectoId;

    /**
     * @param url        URL de la API de Taiga
     * @param usuario    usuario (cuenta bot)
     * @param clave      contraseña de la cuenta bot
     * @param proyectoId id del proyecto
     */
    public ReporteTaigaServicio(@Value("${ferrestock.taiga.url:https://api.taiga.io}") String url,
                                @Value("${ferrestock.taiga.usuario:}") String usuario,
                                @Value("${ferrestock.taiga.clave:}") String clave,
                                @Value("${ferrestock.taiga.proyecto-id:0}") Long proyectoId) {
        this.cliente = RestClient.builder().baseUrl(url + "/api/v1").build();
        this.usuario = usuario;
        this.clave = clave;
        this.proyectoId = proyectoId;
    }

    /** @return si hay credenciales y proyecto configurados */
    public boolean configurado() {
        return !usuario.isBlank() && !clave.isBlank() && proyectoId > 0;
    }

    /**
     * @param asunto      asunto del issue
     * @param descripcion descripción en Markdown
     * @return referencia del issue creado (por ejemplo {@code #42}), o vacío si no está configurado o falló
     */
    @SuppressWarnings("unchecked")
    public Optional<String> crearIssue(String asunto, String descripcion) {
        if (!configurado()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> auth = cliente.post().uri("/auth").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("type", "normal", "username", usuario, "password", clave)).retrieve().body(Map.class);
            String token = (String) auth.get("auth_token");
            Map<String, Object> issue = cliente.post().uri("/issues").contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("project", proyectoId, "subject", asunto, "description", descripcion,
                    "tags", List.of("error-produccion", "backend")))
                .retrieve().body(Map.class);
            return Optional.of("#" + issue.get("ref"));
        } catch (RestClientException | NullPointerException e) {
            log.warn("No fue posible reportar el error en Taiga: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
