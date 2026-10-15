/*
 * Nombre:                 TercerosIT.java
 * Descripción:            Pruebas de integración de clientes y proveedores contra PostgreSQL real: CA-TER-01 a 04, permisos y filtros.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.5.0
 */
package com.ferrestock.terceros;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.ThreadLocalRandom;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ferrestock.ApoyoIT;
import com.ferrestock.compartido.validacion.ValidadorNit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;

class TercerosIT extends ApoyoIT {

    @Autowired private ObjectMapper json;

    private MockHttpSession admin;
    private MockHttpSession operador;

    @BeforeEach
    void preparar() throws Exception {
        admin = sesionDe(crearUsuario("ADMIN"));
        operador = sesionDe(crearUsuario("OPERATIVO"));
    }

    /** Número de documento aleatorio de 10 dígitos para no chocar con otras pruebas. */
    private static String numero() {
        return String.valueOf(ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L));
    }

    private ResultActions crear(MockHttpSession sesion, String persona, String tipo, String numero, String dv,
                                boolean cliente, boolean proveedor) throws Exception {
        String digito = dv == null ? "null" : "\"" + dv + "\"";
        return mvc.perform(post("/api/v1/terceros").with(csrf()).session(sesion).contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"tipoPersona":"%s","tipoDocumento":"%s","numeroDocumento":"%s","digitoVerificacion":%s,
                 "nombre":"Ferretería El Tornillo SAS","telefono":"+57 604 444 5566","correo":"compras@eltornillo.co",
                 "direccion":"Cra 50 # 10-20","esCliente":%s,"esProveedor":%s}"""
                .formatted(persona, tipo, numero, digito, cliente, proveedor)));
    }

    @Test
    @DisplayName("CA-TER-01: una empresa proveedora pasa a ser también cliente sin duplicarse")
    void clienteYProveedor() throws Exception {
        String nit = numero().substring(0, 9);
        String dv = String.valueOf(ValidadorNit.calcularDigito(nit));
        long id = json.readTree(crear(operador, "JURIDICA", "NIT", nit, dv, false, true)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.esProveedor").value(true))
            .andExpect(jsonPath("$.esCliente").value(false))
            .andExpect(jsonPath("$.digitoVerificacion").value(dv))
            .andReturn().getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(put("/api/v1/terceros/" + id).with(csrf()).session(operador).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Ferretería El Tornillo SAS\",\"esCliente\":true,\"esProveedor\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.esCliente").value(true)).andExpect(jsonPath("$.esProveedor").value(true))
            .andExpect(jsonPath("$.numeroDocumento").value(nit));

        mvc.perform(get("/api/v1/terceros").session(operador).param("q", nit).param("rol", "CLIENTE"))
            .andExpect(jsonPath("$.totalElementos").value(1));
        mvc.perform(get("/api/v1/terceros").session(operador).param("q", nit).param("rol", "PROVEEDOR"))
            .andExpect(jsonPath("$.totalElementos").value(1));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tercero WHERE numero_documento = ?", Integer.class, nit))
            .isEqualTo(1);
        assertThat(eventos("TERCERO_ACTUALIZADO", "cliente y proveedor")).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("CA-TER-02: el mismo tipo y número de documento responde 409 en el campo numeroDocumento")
    void documentoDuplicado() throws Exception {
        String cc = numero();
        crear(operador, "NATURAL", "CC", cc, null, true, false).andExpect(status().isCreated());
        crear(admin, "NATURAL", "CC", cc, null, false, true)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("DUPLICADO"))
            .andExpect(jsonPath("$.errores[0].campo").value("numeroDocumento"));
        // Otro tipo de documento con el mismo número sí se admite
        crear(operador, "NATURAL", "CE", cc, null, true, false).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("CA-TER-03: una persona jurídica con CC se rechaza en el campo tipoDocumento")
    void juridicaConCc() throws Exception {
        crear(operador, "JURIDICA", "CC", numero(), null, true, false)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("PERSONA_DOCUMENTO_INCOMPATIBLE"))
            .andExpect(jsonPath("$.errores[0].campo").value("tipoDocumento"));
    }

    @Test
    @DisplayName("CA-TER-04: un NIT con DV incorrecto o sin DV se rechaza en el campo digitoVerificacion")
    void nitConDvIncorrecto() throws Exception {
        String nit = numero().substring(0, 9);
        String malo = String.valueOf((ValidadorNit.calcularDigito(nit) + 1) % 10);
        crear(operador, "JURIDICA", "NIT", nit, malo, true, false)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("DIGITO_VERIFICACION_INVALIDO"))
            .andExpect(jsonPath("$.errores[0].campo").value("digitoVerificacion"));
        crear(operador, "NATURAL", "NIT", nit, null, true, false)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores[0].campo").value("digitoVerificacion"));
    }

    @Test
    @DisplayName("Validaciones: sin rol, número con letras, correo y teléfono inválidos; un error por campo")
    void validaciones() throws Exception {
        crear(operador, "NATURAL", "CC", numero(), null, false, false)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("ROL_TERCERO_REQUERIDO"));
        mvc.perform(post("/api/v1/terceros").with(csrf()).session(operador).contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipoPersona\":\"NATURAL\",\"tipoDocumento\":\"CC\",\"numeroDocumento\":\"12.345.678\","
                    + "\"nombre\":\"Ana\",\"telefono\":\"abc\",\"correo\":\"no-es-correo\",\"esCliente\":true}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.length()").value(3));
    }

    @Test
    @DisplayName("RF-TER-006: solo el ADMIN desactiva; el tercero inactivo se filtra y se conserva")
    void desactivarSoloAdmin() throws Exception {
        String cc = numero();
        long id = json.readTree(crear(operador, "NATURAL", "CC", cc, null, true, false).andReturn().getResponse()
            .getContentAsString()).get("id").asLong();
        mvc.perform(patch("/api/v1/terceros/" + id + "/estado").with(csrf()).session(operador)
                .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
            .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/terceros/" + id + "/estado").with(csrf()).session(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));
        mvc.perform(get("/api/v1/terceros").session(operador).param("q", cc).param("activo", "true"))
            .andExpect(jsonPath("$.totalElementos").value(0));
        mvc.perform(get("/api/v1/terceros/" + id).session(operador)).andExpect(jsonPath("$.activo").value(false));
        assertThat(eventos("TERCERO_DESACTIVADO", cc)).isEqualTo(1);
    }

    @Test
    @DisplayName("El documento no se modifica al editar, aunque se envíe en el cuerpo")
    void documentoInmutable() throws Exception {
        String cc = numero();
        long id = json.readTree(crear(operador, "NATURAL", "CC", cc, null, true, false).andReturn().getResponse()
            .getContentAsString()).get("id").asLong();
        mvc.perform(put("/api/v1/terceros/" + id).with(csrf()).session(operador).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Ana María\",\"numeroDocumento\":\"99999\",\"tipoDocumento\":\"NIT\","
                    + "\"esCliente\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.numeroDocumento").value(cc))
            .andExpect(jsonPath("$.tipoDocumento").value("CC"));
    }
}
