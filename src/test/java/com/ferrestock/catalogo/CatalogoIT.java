/*
 * Nombre:                 CatalogoIT.java
 * Descripción:            Pruebas de integración del catálogo contra PostgreSQL real: CA-PRO-01 a 05, 08, 10 y CA-PRE-01, 02, permisos y formato de montos.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.4.0
 */
package com.ferrestock.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ferrestock.ApoyoIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;

class CatalogoIT extends ApoyoIT {

    @Autowired private ObjectMapper json;

    private MockHttpSession admin;
    private MockHttpSession operador;
    private long categoriaId;

    @BeforeEach
    void preparar() throws Exception {
        admin = sesionDe(crearUsuario("ADMIN"));
        operador = sesionDe(crearUsuario("OPERATIVO"));
        categoriaId = crearCategoria("Prueba " + sufijo());
    }

    private static String sufijo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private long crearCategoria(String nombre) throws Exception {
        String cuerpo = mvc.perform(post("/api/v1/categorias").with(csrf()).session(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"%s\"}".formatted(nombre)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(cuerpo).get("id").asLong();
    }

    private ResultActions crearProducto(MockHttpSession sesion, String codigo, long categoria, String precio)
            throws Exception {
        return mvc.perform(post("/api/v1/productos").with(csrf()).session(sesion).contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"codigo":"%s","nombre":"Martillo de uña 16 oz","descripcion":"Mango de fibra","categoriaId":%d,
                 "unidadMedida":"UNIDAD","permiteFraccion":false,"precioVenta":"%s"}""".formatted(codigo, categoria, precio)));
    }

    private JsonNode cuerpo(ResultActions resultado) throws Exception {
        return json.readTree(resultado.andReturn().getResponse().getContentAsString());
    }

    @Test
    @DisplayName("CA-PRO-01: un OPERATIVO crea un producto ACTIVO; los montos viajan como texto y no se expone el costo")
    void crearProducto() throws Exception {
        String codigo = "fer-" + sufijo();
        crearProducto(operador, codigo, categoriaId, "35000")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.codigo").value(codigo.toUpperCase()))
            .andExpect(jsonPath("$.estado").value("ACTIVO"))
            .andExpect(jsonPath("$.precioVenta").value("35000.00"))
            .andExpect(jsonPath("$.existencias").value("0.000"))
            .andExpect(jsonPath("$.costoPromedio").doesNotExist())
            .andExpect(jsonPath("$.categoria.id").value(categoriaId));
        assertThat(eventos("PRODUCTO_CREADO", codigo.toUpperCase())).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-PRO-02: datos inválidos responden 400 con un error por campo, en el idioma pedido")
    void crearInvalido() throws Exception {
        mvc.perform(post("/api/v1/productos").with(csrf()).session(operador).header("Accept-Language", "en")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"con espacio\",\"nombre\":\"\",\"categoriaId\":%d,\"unidadMedida\":\"UNIDAD\","
                    .formatted(categoriaId) + "\"precioVenta\":\"-5\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.length()").value(3))
            .andExpect(jsonPath("$.errores[?(@.campo=='precioVenta')].mensaje").value("The price must be greater than zero."));
        mvc.perform(post("/api/v1/productos").with(csrf()).session(operador).contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"X-1\",\"nombre\":\"Xx\",\"categoriaId\":%d,\"unidadMedida\":\"DOCENA\",".formatted(categoriaId)
                    + "\"precioVenta\":\"1\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CA-PRO-03: un código duplicado (sin importar mayúsculas) responde 409 en el campo codigo")
    void codigoDuplicado() throws Exception {
        String codigo = "DUP-" + sufijo();
        crearProducto(operador, codigo, categoriaId, "100").andExpect(status().isCreated());
        crearProducto(admin, codigo.toLowerCase(), categoriaId, "200")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("DUPLICADO"))
            .andExpect(jsonPath("$.errores[0].campo").value("codigo"));
    }

    @Test
    @DisplayName("CA-PRO-04: listado filtrado por categoría y estado con paginación; 404 para un id inexistente")
    void listadoConFiltros() throws Exception {
        long categoria = crearCategoria("Herramientas " + sufijo());
        long otra = crearCategoria("Otra " + sufijo());
        for (int i = 0; i < 12; i++) {
            long id = cuerpo(crearProducto(operador, "H-%02d-%s".formatted(i, sufijo()), categoria, "1000")).get("id").asLong();
            if (i < 3) {
                mvc.perform(patch("/api/v1/productos/" + id + "/estado").with(csrf()).session(operador)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"INACTIVO\"}"))
                    .andExpect(status().isOk());
            }
        }
        for (int i = 0; i < 18; i++) {
            crearProducto(operador, "O-%02d-%s".formatted(i, sufijo()), otra, "1000");
        }

        mvc.perform(get("/api/v1/productos").session(operador).param("categoriaId", String.valueOf(categoria))
                .param("estado", "ACTIVO").param("pagina", "0").param("tamano", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contenido.length()").value(5))
            .andExpect(jsonPath("$.totalElementos").value(9))
            .andExpect(jsonPath("$.totalPaginas").value(2));

        mvc.perform(get("/api/v1/productos").session(operador).param("q", "h-00").param("categoriaId",
                String.valueOf(categoria)))
            .andExpect(jsonPath("$.totalElementos").value(1));
        mvc.perform(get("/api/v1/productos").session(operador).param("orden", "costoPromedio,desc"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/productos/999999999").session(operador)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CA-PRO-05: editar conserva el precio y las existencias; una versión vieja responde 409")
    void editarConVersion() throws Exception {
        JsonNode creado = cuerpo(crearProducto(operador, "ED-" + sufijo(), categoriaId, "35000"));
        long id = creado.get("id").asLong();
        long version = creado.get("version").asLong();
        String edicion = """
            {"nombre":"Martillo de uña","descripcion":"Mango de madera","categoriaId":%d,"unidadMedida":"UNIDAD",
             "permiteFraccion":false,"version":%d}""";

        mvc.perform(put("/api/v1/productos/" + id).with(csrf()).session(operador).contentType(MediaType.APPLICATION_JSON)
                .content(edicion.formatted(categoriaId, version)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.descripcion").value("Mango de madera"))
            .andExpect(jsonPath("$.precioVenta").value("35000.00"))
            .andExpect(jsonPath("$.version").value(version + 1));

        mvc.perform(put("/api/v1/productos/" + id).with(csrf()).session(admin).contentType(MediaType.APPLICATION_JSON)
                .content(edicion.formatted(categoriaId, version)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("VERSION_DESACTUALIZADA"));
    }

    @Test
    @DisplayName("CA-PRO-06/08: desactivar conserva el producto (no hay DELETE) y reactivar lo deja ACTIVO")
    void desactivarYReactivar() throws Exception {
        long id = cuerpo(crearProducto(operador, "DS-" + sufijo(), categoriaId, "100")).get("id").asLong();
        mvc.perform(patch("/api/v1/productos/" + id + "/estado").with(csrf()).session(operador)
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"INACTIVO\"}"))
            .andExpect(jsonPath("$.estado").value("INACTIVO"));
        mvc.perform(get("/api/v1/productos/" + id).session(operador)).andExpect(jsonPath("$.estado").value("INACTIVO"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/productos/" + id)
                .with(csrf()).session(admin))
            .andExpect(status().isMethodNotAllowed());
        mvc.perform(patch("/api/v1/productos/" + id + "/estado").with(csrf()).session(operador)
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"ACTIVO\"}"))
            .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    @DisplayName("CA-PRE-01/02: el cambio de precio se audita; un precio de 0 o no numérico responde 400")
    void cambiarPrecio() throws Exception {
        String codigo = "PR-" + sufijo();
        long id = cuerpo(crearProducto(operador, codigo, categoriaId, "2500")).get("id").asLong();
        mvc.perform(patch("/api/v1/productos/" + id + "/precio").with(csrf()).session(operador)
                .contentType(MediaType.APPLICATION_JSON).content("{\"precioVenta\":\"2800\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.precioVenta").value("2800.00"));
        assertThat(eventos("PRECIO_MODIFICADO", codigo.toUpperCase() + " de 2500.00 a 2800.00")).isEqualTo(1);

        for (String invalido : new String[] {"\"0\"", "\"abc\"", "\"12.345\""}) {
            mvc.perform(patch("/api/v1/productos/" + id + "/precio").with(csrf()).session(operador)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"precioVenta\":" + invalido + "}"))
                .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/v1/productos/" + id).session(operador)).andExpect(jsonPath("$.precioVenta").value("2800.00"));
    }

    @Test
    @DisplayName("CA-PRO-10: categorías únicas sin importar mayúsculas; solo el ADMIN las gestiona; inactivas no se asignan")
    void categorias() throws Exception {
        String nombre = "Pinturas " + sufijo();
        crearCategoria(nombre);
        mvc.perform(post("/api/v1/categorias").with(csrf()).session(admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"%s\"}".formatted(nombre.toLowerCase())))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.errores[0].campo").value("nombre"));
        mvc.perform(post("/api/v1/categorias").with(csrf()).session(operador).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Intento\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/categorias").session(operador)).andExpect(status().isOk());

        mvc.perform(patch("/api/v1/categorias/" + categoriaId + "/estado").with(csrf()).session(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"activa\":false}"))
            .andExpect(jsonPath("$.activa").value(false));
        crearProducto(operador, "CI-" + sufijo(), categoriaId, "100")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("CATEGORIA_INACTIVA"))
            .andExpect(jsonPath("$.errores[0].campo").value("categoriaId"));
        mvc.perform(get("/api/v1/categorias").session(operador).param("activa", "true"))
            .andExpect(jsonPath("$[?(@.id==%d)]".formatted(categoriaId)).isEmpty());
    }

    @Test
    @DisplayName("DP-15: el costo y la valorización solo los consulta el ADMIN")
    void costoSoloAdmin() throws Exception {
        long id = cuerpo(crearProducto(operador, "CO-" + sufijo(), categoriaId, "100")).get("id").asLong();
        mvc.perform(get("/api/v1/productos/" + id + "/costo").session(operador)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/productos/" + id + "/costo").session(admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.costoPromedio").value("0.0000"))
            .andExpect(jsonPath("$.valorInventario").value("0.00"));
    }
}
