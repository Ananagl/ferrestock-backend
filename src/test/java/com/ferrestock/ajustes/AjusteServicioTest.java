/*
 * Nombre:                 AjusteServicioTest.java
 * Descripción:            Pruebas unitarias con Mockito del registro de ajustes: rol, validaciones previas, costo de las salidas y auditoría.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.7.0
 */
package com.ferrestock.ajustes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import com.ferrestock.ajustes.dto.respuesta.AjusteResponse;
import com.ferrestock.ajustes.dto.solicitud.AjusteCrearRequest;
import com.ferrestock.ajustes.entidad.Ajuste;
import com.ferrestock.ajustes.entidad.TipoAjuste;
import com.ferrestock.ajustes.repositorio.AjusteRepositorio;
import com.ferrestock.ajustes.servicio.AjusteServicio;
import com.ferrestock.auditoria.entidad.AccionAuditoria;
import com.ferrestock.auditoria.servicio.AuditoriaServicio;
import com.ferrestock.catalogo.entidad.EstadoProducto;
import com.ferrestock.catalogo.entidad.Producto;
import com.ferrestock.catalogo.servicio.ProductoServicio;
import com.ferrestock.compartido.documentos.TipoDocumentoComercial;
import com.ferrestock.compartido.error.CodigoError;
import com.ferrestock.compartido.error.NegocioException;
import com.ferrestock.compartido.seguridad.UsuarioAutenticado;
import com.ferrestock.comprobantes.servicio.ConsecutivoServicio;
import com.ferrestock.inventario.entidad.MovimientoInventario;
import com.ferrestock.inventario.entidad.TipoMovimiento;
import com.ferrestock.inventario.servicio.MovimientoInventarioServicio;
import com.ferrestock.usuarios.servicio.UsuarioServicio;
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
class AjusteServicioTest {

    @Mock private AjusteRepositorio ajustes;
    @Mock private ProductoServicio productos;
    @Mock private MovimientoInventarioServicio movimientos;
    @Mock private ConsecutivoServicio consecutivos;
    @Mock private UsuarioServicio usuarios;
    @Mock private AuditoriaServicio auditoria;
    private AjusteServicio servicio;
    private Producto producto;

    @BeforeEach
    void preparar() {
        servicio = new AjusteServicio(ajustes, productos, movimientos, consecutivos, usuarios, auditoria,
            Clock.fixed(Instant.parse("2026-10-15T15:00:00Z"), ZoneOffset.UTC));
        producto = Producto.builder().id(7L).codigo("FER-007").nombre("Martillo").estado(EstadoProducto.ACTIVO)
            .existencias(new BigDecimal("8.000")).costoPromedio(new BigDecimal("1300.0000")).build();
        when(productos.bloquearParaMovimiento(anyCollection())).thenReturn(Map.of(7L, producto));
        when(movimientos.validarCantidad(any(), any(), any())).thenAnswer(inv -> inv.getArgument(1));
        when(consecutivos.siguiente(TipoDocumentoComercial.AJUSTE)).thenReturn("AJU-000001");
        when(ajustes.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        MovimientoInventario movimiento = MovimientoInventario.builder().saldoResultante(new BigDecimal("7.000"))
            .costoPromedioResultante(new BigDecimal("1300.0000")).build();
        when(movimientos.registrarSalida(any(), any(), any(), any(), any())).thenReturn(movimiento);
        when(movimientos.registrarEntrada(any(), any(), any(), any(), eq(true), any(), any())).thenReturn(movimiento);
        autenticar("ADMIN");
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private static void autenticar(String rol) {
        UsuarioAutenticado u = new UsuarioAutenticado(1L, "admin.demo", "Admin", rol, null, true, null, false, "es", "s");
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(u, null, u.getAuthorities()));
    }

    private static AjusteCrearRequest solicitud(TipoAjuste tipo, String cantidad, String costo) {
        return new AjusteCrearRequest(7L, tipo, new BigDecimal(cantidad), costo == null ? null : new BigDecimal(costo),
            "  Unidad dañada en bodega  ");
    }

    @Test
    @DisplayName("Un operativo no puede registrar ajustes aunque llame al servicio directamente (RN-08)")
    void soloAdmin() {
        autenticar("OPERATIVO");
        assertThatThrownBy(() -> servicio.registrar(solicitud(TipoAjuste.SALIDA, "1", null)))
            .extracting("codigo").isEqualTo(CodigoError.ACCESO_DENEGADO);
        verifyNoInteractions(productos, consecutivos, ajustes, movimientos);
    }

    @Test
    @DisplayName("Una entrada sin costo se rechaza antes de consumir el consecutivo")
    void entradaSinCosto() {
        assertThatThrownBy(() -> servicio.registrar(solicitud(TipoAjuste.ENTRADA, "5", null)))
            .isInstanceOfSatisfying(NegocioException.class, e -> {
                assertThat(e.getCodigo()).isEqualTo(CodigoError.COSTO_UNITARIO_REQUERIDO);
                assertThat(e.getCampo()).isEqualTo("costoUnitario");
            });
        verify(consecutivos, never()).siguiente(any());
    }

    @Test
    @DisplayName("Un producto inactivo no admite ajustes (RF-PRO-008)")
    void productoInactivo() {
        producto.setEstado(EstadoProducto.INACTIVO);
        assertThatThrownBy(() -> servicio.registrar(solicitud(TipoAjuste.ENTRADA, "5", "10")))
            .extracting("codigo").isEqualTo(CodigoError.PRODUCTO_INACTIVO);
    }

    @Test
    @DisplayName("Una salida mayor que las existencias se rechaza en el campo cantidad sin tocar el kardex")
    void salidaInsuficiente() {
        assertThatThrownBy(() -> servicio.registrar(solicitud(TipoAjuste.SALIDA, "9", null)))
            .isInstanceOfSatisfying(NegocioException.class, e -> {
                assertThat(e.getCodigo()).isEqualTo(CodigoError.EXISTENCIAS_INSUFICIENTES);
                assertThat(e.getCampo()).isEqualTo("cantidad");
            });
        verifyNoInteractions(ajustes);
        verify(movimientos, never()).registrarSalida(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Una salida guarda el CPP vigente como costo, ignora el costo enviado, recorta el motivo y audita")
    void salidaAlCostoVigente() {
        AjusteResponse r = servicio.registrar(solicitud(TipoAjuste.SALIDA, "1", "99999"));

        verify(ajustes).saveAndFlush(argThat((Ajuste a) -> a.getCostoUnitario().compareTo(new BigDecimal("1300")) == 0
            && a.getMotivo().equals("Unidad dañada en bodega") && a.getConsecutivo().equals("AJU-000001")));
        verify(movimientos).registrarSalida(eq(producto), eq(new BigDecimal("1")), eq(TipoMovimiento.AJUSTE_SALIDA),
            eq(TipoDocumentoComercial.AJUSTE), any());
        verify(auditoria).registrar(eq(AccionAuditoria.AJUSTE_REGISTRADO), contains("AJU-000001"));
        assertThat(r.existenciasResultantes()).isEqualByComparingTo("7");
        assertThat(r.usuario()).isEqualTo("admin.demo");
    }

    @Test
    @DisplayName("Una entrada pondera el CPP con el costo indicado (DP-08)")
    void entradaPondera() {
        servicio.registrar(solicitud(TipoAjuste.ENTRADA, "2", "1500"));
        verify(movimientos).registrarEntrada(eq(producto), eq(new BigDecimal("2")), eq(new BigDecimal("1500.0000")),
            eq(TipoMovimiento.AJUSTE_ENTRADA), eq(true), eq(TipoDocumentoComercial.AJUSTE), any());
    }
}
