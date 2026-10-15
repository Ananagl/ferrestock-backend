/*
 * Nombre:                 RecuerdameServicio.java
 * Descripción:            Sesión persistente (Recordarme) con tokens rotativos guardados en persistent_logins (RF-AUT-002).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.2.0
 */
package com.ferrestock.autenticacion.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

/**
 * Variante de {@link PersistentTokenBasedRememberMeServices} para un login JSON: la solicitud de "Recordarme" no
 * llega como parámetro de formulario sino en el cuerpo, y el servicio de autenticación la marca como atributo.
 *
 * <p>Cada uso rota el token; si se presenta una serie válida con un token viejo (posible robo), Spring Security
 * invalida todos los tokens del usuario.</p>
 */
public class RecuerdameServicio extends PersistentTokenBasedRememberMeServices {

    /** Nombre de la cookie de sesión persistente. */
    public static final String COOKIE = "FSRECUERDAME";

    /** Atributo de la petición que indica que el usuario marcó "Recordarme". */
    public static final String ATRIBUTO_SOLICITADO = RecuerdameServicio.class.getName() + ".solicitado";

    /**
     * @param clave        clave de firma
     * @param usuarios     servicio de carga de usuarios
     * @param repositorio  repositorio JDBC de tokens
     * @param dias         días de validez
     * @param cookieSegura si la cookie lleva el atributo Secure
     */
    public RecuerdameServicio(String clave, UserDetailsService usuarios, PersistentTokenRepository repositorio,
                              int dias, boolean cookieSegura) {
        super(clave, usuarios, repositorio);
        setCookieName(COOKIE);
        setTokenValiditySeconds(dias * 24 * 60 * 60);
        setUseSecureCookie(cookieSegura);
    }

    @Override
    protected boolean rememberMeRequested(HttpServletRequest solicitud, String parametro) {
        return Boolean.TRUE.equals(solicitud.getAttribute(ATRIBUTO_SOLICITADO));
    }
}
