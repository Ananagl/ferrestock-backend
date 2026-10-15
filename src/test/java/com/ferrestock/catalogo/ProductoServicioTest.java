/*
 * Nombre:                 ProductoServicioTest.java
 * Descripción:            Pruebas unitarias con Mockito de las reglas del catálogo de productos y precios.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.4.0
 */
package com.ferrestock.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import com.ferrestock.auditoria.entidad.AccionAuditoria;
import com.ferrestock.auditoria.servicio.AuditoriaServicio;
import com.ferrestock.catalogo.dto.solicitud.ProductoActualizarRequest;
import com.ferrestock.catalogo.dto.solicitud.ProductoCrearRequest;
import com.ferrestock.catalogo.entidad.Categoria;
import com.ferrestock.catalogo.entidad.EstadoProducto;
import com.ferrestock.catalogo.entidad.Producto;
import com.ferrestock.catalogo.entidad.UnidadMedida;
import com.ferrestock.catalogo.repositorio.ProductoRepositorio;
import com.ferrestock.catalogo.servicio.CategoriaServicio;
import com.ferrestock.catalogo.servicio.ProductoServicio;
import com.ferrestock.compartido.error.CodigoError;
import com.ferrestock.compartido.error.NegocioException;
import com.ferrestock.compartido.seguridad.UsuarioAutenticado;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ProductoServicioTest {

    @Mock private ProductoRepositorio productos;
    @Mock private CategoriaServicio categorias;
    @Mock private AuditoriaServicio auditoria;

    private ProductoServicio servicio;
    private final Categoria herramientas = Categoria.builder().id(1L).nombre("Herramientas manuales").build();

    @BeforeEach
    void preparar() {
        servicio = new ProductoServicio(productos, categorias, auditoria);
        UsuarioAutenticado operador = new UsuarioAutenticado(7L, "operador", "Operador", "OPERATIVO", null, true, null,
            false, "es", "sello");
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(operador, null, operador.getAuthorities()));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private Producto martillo() {
        return Producto.builder().id(10L).codigo("FER-001").nombre("Martillo de uña 16 oz").categoria(herramientas)
            .unidadMedida(UnidadMedida.UNIDAD).precioVenta(new BigDecimal("35000.00")).version(3L).build();
    }

    @Test
    @DisplayName("CA-PRO-01: crea en mayúsculas, ACTIVO, con existencias y costo en cero, y audita")
    void crearValido() {
        when(productos.findByCodigoIgnoreCase("FER-001")).thenReturn(Optional.empty());
        when(categorias.activaParaAsignar(1L)).thenReturn(herramientas);
        when(productos.saveAndFlush(any())).thenAnswer(inv -> {
            Producto p = inv.getArgument(0);
            p.setId(10L);
            p.setVersion(0L);
            return p;
        });

        var creado = servicio.crear(new ProductoCrearRequest(" fer-001 ", " Martillo de uña 16 oz ", "  ", 1L,
            UnidadMedida.UNIDAD, false, new BigDecimal("35000")));

        assertThat(creado.codigo()).isEqualTo("FER-001");
        assertThat(creado.nombre()).isEqualTo("Martillo de uña 16 oz");
        assertThat(creado.descripcion()).isNull();
        assertThat(creado.estado()).isEqualTo("ACTIVO");
        assertThat(creado.existencias()).isEqualByComparingTo("0");
        assertThat(creado.precioVenta()).isEqualByComparingTo("35000.00").hasScaleOf(2);
        verify(auditoria).registrar(eq(AccionAuditoria.PRODUCTO_CREADO), any());
    }

    @Test
    @DisplayName("CA-PRO-03: un código existente (sin importar mayúsculas) es DUPLICADO en el campo codigo")
    void codigoDuplicado() {
        when(productos.findByCodigoIgnoreCase("FER-001")).thenReturn(Optional.of(martillo()));
        assertThatThrownBy(() -> servicio.crear(new ProductoCrearRequest("fer-001", "Otro", null, 1L,
            UnidadMedida.UNIDAD, false, BigDecimal.TEN)))
            .isInstanceOfSatisfying(NegocioException.class, e -> {
                assertThat(e.getCodigo()).isEqualTo(CodigoError.DUPLICADO);
                assertThat(e.getCampo()).isEqualTo("codigo");
            });
        verify(productos, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Editar con una versión vieja responde VERSION_DESACTUALIZADA y no guarda")
    void versionDesactualizada() {
        when(productos.findById(10L)).thenReturn(Optional.of(martillo()));
        assertThatThrownBy(() -> servicio.actualizar(10L, new ProductoActualizarRequest("Martillo", null, 1L,
            UnidadMedida.UNIDAD, false, 2L)))
            .isInstanceOfSatisfying(NegocioException.class,
                e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.VERSION_DESACTUALIZADA));
        verify(productos, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("CA-PRO-05: editar no toca existencias, costo ni precio y solo valida la categoría si cambia")
    void editarNoTocaExistencias() {
        Producto producto = martillo();
        producto.setExistencias(new BigDecimal("8.000"));
        producto.setCostoPromedio(new BigDecimal("21000.0000"));
        when(productos.findById(10L)).thenReturn(Optional.of(producto));
        when(productos.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        var editado = servicio.actualizar(10L, new ProductoActualizarRequest("Martillo de uña", "Mango de fibra", 1L,
            UnidadMedida.UNIDAD, false, 3L));

        assertThat(editado.descripcion()).isEqualTo("Mango de fibra");
        assertThat(producto.getExistencias()).isEqualByComparingTo("8");
        assertThat(producto.getCostoPromedio()).isEqualByComparingTo("21000");
        assertThat(producto.getPrecioVenta()).isEqualByComparingTo("35000");
        verify(categorias, never()).activaParaAsignar(any());
    }

    @Test
    @DisplayName("CA-PRE-01: cambiar el precio lo redondea a 2 decimales y audita el valor anterior y el nuevo")
    void cambiarPrecioAudita() {
        when(productos.findById(10L)).thenReturn(Optional.of(martillo()));
        when(productos.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        var resultado = servicio.cambiarPrecio(10L, new BigDecimal("38000.005"));

        assertThat(resultado.precioVenta()).isEqualByComparingTo("38000.01");
        verify(auditoria).registrar(eq(AccionAuditoria.PRECIO_MODIFICADO),
            argThat(texto -> texto.contains("FER-001") && texto.contains("35000.00") && texto.contains("38000.01")));
    }

    @Test
    @DisplayName("Un precio igual al vigente no genera evento de auditoría")
    void precioIgualNoAudita() {
        when(productos.findById(10L)).thenReturn(Optional.of(martillo()));
        servicio.cambiarPrecio(10L, new BigDecimal("35000"));
        verify(auditoria, never()).registrar(any(), any());
    }

    @Test
    @DisplayName("CA-PRO-06/08: desactivar y reactivar cambian el estado y se auditan")
    void desactivarYReactivar() {
        Producto producto = martillo();
        when(productos.findById(10L)).thenReturn(Optional.of(producto));
        when(productos.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(servicio.cambiarEstado(10L, EstadoProducto.INACTIVO).estado()).isEqualTo("INACTIVO");
        assertThat(servicio.cambiarEstado(10L, EstadoProducto.ACTIVO).estado()).isEqualTo("ACTIVO");
        verify(auditoria).registrar(eq(AccionAuditoria.PRODUCTO_DESACTIVADO), any());
        verify(auditoria).registrar(eq(AccionAuditoria.PRODUCTO_ACTIVADO), any());
    }

    @Test
    @DisplayName("El valor del inventario es existencias × costo promedio, a 2 decimales")
    void costoYValor() {
        Producto producto = martillo();
        producto.setExistencias(new BigDecimal("40.000"));
        producto.setCostoPromedio(new BigDecimal("1300.0000"));
        when(productos.findById(10L)).thenReturn(Optional.of(producto));

        var costo = servicio.costo(10L);
        assertThat(costo.valorInventario()).isEqualByComparingTo("52000.00").hasScaleOf(2);
    }
}
