/*
 * Nombre:                 MovimientoInventarioServicioTest.java
 * Descripción:            Pruebas unitarias con Mockito de entradas, salidas y validación de cantidades del kardex.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.6.0
 */
package com.ferrestock.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.ferrestock.catalogo.entidad.Producto;
import com.ferrestock.compartido.documentos.TipoDocumentoComercial;
import com.ferrestock.compartido.error.CodigoError;
import com.ferrestock.compartido.error.NegocioException;
import com.ferrestock.compartido.seguridad.UsuarioAutenticado;
import com.ferrestock.inventario.entidad.MovimientoInventario;
import com.ferrestock.inventario.entidad.TipoMovimiento;
import com.ferrestock.inventario.repositorio.MovimientoInventarioRepositorio;
import com.ferrestock.inventario.servicio.MovimientoInventarioServicio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MovimientoInventarioServicioTest {

    @Mock private MovimientoInventarioRepositorio repositorio;
    private MovimientoInventarioServicio servicio;

    @BeforeEach
    void preparar() {
        servicio = new MovimientoInventarioServicio(repositorio, Clock.fixed(Instant.parse("2026-10-15T15:00:00Z"), ZoneOffset.UTC));
        when(repositorio.save(any())).thenAnswer(inv -> inv.getArgument(0));
        UsuarioAutenticado admin = new UsuarioAutenticado(1L, "admin", "Admin", "ADMIN", null, true, null, false, "es", "s");
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(admin, null, admin.getAuthorities()));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private static Producto producto(String existencias, String costo, boolean fraccion) {
        return Producto.builder().id(5L).codigo("FER-001").permiteFraccion(fraccion)
            .existencias(new BigDecimal(existencias)).costoPromedio(new BigDecimal(costo)).build();
    }

    @Test
    @DisplayName("Una entrada ponderada actualiza saldo y CPP y guarda el CPP anterior")
    void entradaPonderada() {
        Producto p = producto("10.000", "1000.0000", false);
        MovimientoInventario m = servicio.registrarEntrada(p, new BigDecimal("30.000"), new BigDecimal("1400"),
            TipoMovimiento.COMPRA, true, TipoDocumentoComercial.COMPRA, 1L);

        assertThat(p.getExistencias()).isEqualTo("40.000");
        assertThat(p.getCostoPromedio()).isEqualTo("1300.0000");
        assertThat(m.getCostoPromedioAnterior()).isEqualTo("1000.0000");
        assertThat(m.getSaldoResultante()).isEqualTo("40.000");
        assertThat(m.getUsuarioId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Una entrada sin ponderar (devolución sin venta) entra al CPP vigente y no lo cambia")
    void entradaSinPonderar() {
        Producto p = producto("10.000", "1200.0000", false);
        MovimientoInventario m = servicio.registrarEntrada(p, new BigDecimal("2.000"), new BigDecimal("1"),
            TipoMovimiento.DEVOLUCION, false, TipoDocumentoComercial.DEVOLUCION, 1L);
        assertThat(p.getCostoPromedio()).isEqualTo("1200.0000");
        assertThat(m.getCostoUnitario()).isEqualTo("1200.0000");
        assertThat(p.getExistencias()).isEqualTo("12.000");
    }

    @Test
    @DisplayName("Una salida hasta cero se permite y conserva el CPP como último costo conocido")
    void salidaHastaCero() {
        Producto p = producto("3.000", "1300.0000", false);
        MovimientoInventario m = servicio.registrarSalida(p, new BigDecimal("3.000"), TipoMovimiento.VENTA,
            TipoDocumentoComercial.VENTA, 1L);
        assertThat(p.getExistencias()).isEqualTo("0.000");
        assertThat(p.getCostoPromedio()).isEqualTo("1300.0000");
        assertThat(m.getCostoUnitario()).isEqualTo("1300.0000");
    }

    @Test
    @DisplayName("CA-VEN-02/CA-INV-02: una salida mayor que las existencias se rechaza sin tocar el producto")
    void salidaInsuficiente() {
        Producto p = producto("3.000", "1300.0000", false);
        assertThatThrownBy(() -> servicio.registrarSalida(p, new BigDecimal("5.000"), TipoMovimiento.VENTA,
            TipoDocumentoComercial.VENTA, 1L))
            .isInstanceOfSatisfying(NegocioException.class, e -> {
                assertThat(e.getCodigo()).isEqualTo(CodigoError.EXISTENCIAS_INSUFICIENTES);
                assertThat(e.getArgumentos()).containsExactly("FER-001", "3");
            });
        assertThat(p.getExistencias()).isEqualTo("3.000");
        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("CA-PRO-09: un producto que no se vende en fracciones rechaza 1,5; uno fraccionable acepta 2,5")
    void fracciones() {
        assertThatThrownBy(() -> servicio.validarCantidad(producto("10", "0", false), new BigDecimal("1.5"), "cantidad"))
            .isInstanceOfSatisfying(NegocioException.class,
                e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.CANTIDAD_FRACCIONARIA_NO_PERMITIDA));
        assertThat(servicio.validarCantidad(producto("10", "0", true), new BigDecimal("2.5"), "cantidad"))
            .isEqualTo("2.500");
        assertThat(servicio.validarCantidad(producto("10", "0", false), new BigDecimal("4.000"), "cantidad"))
            .isEqualTo("4.000");
    }

    @Test
    @DisplayName("Cantidades cero, negativas o con más de 3 decimales son CANTIDAD_INVALIDA")
    void cantidadesInvalidas() {
        for (String valor : new String[] {"0", "-1", "1.0005"}) {
            assertThatThrownBy(() -> servicio.validarCantidad(producto("10", "0", true), new BigDecimal(valor), "cantidad"))
                .isInstanceOfSatisfying(NegocioException.class,
                    e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.CANTIDAD_INVALIDA));
        }
    }

    @Test
    @DisplayName("Un tipo de movimiento con naturaleza equivocada es un error de programación")
    void naturalezaEquivocada() {
        Producto p = producto("10", "0", false);
        assertThatThrownBy(() -> servicio.registrarEntrada(p, BigDecimal.ONE, BigDecimal.ONE, TipoMovimiento.VENTA, true,
            TipoDocumentoComercial.VENTA, 1L)).isInstanceOf(IllegalArgumentException.class);
    }
}
