/*
 * Nombre:                 I18nConfig.java
 * Descripción:            Internacionalización del backend: idioma según Accept-Language (es por defecto, en) y
 *                         mensajes de validación resueltos desde los archivos i18n/mensajes*.properties.
 * Fecha de creación:      2026-10-14
 * Fecha de actualización: 2026-10-14
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.1.0
 */
package com.ferrestock.configuracion;

import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * Configuración de idiomas (RNF-USA-01).
 */
@Configuration
public class I18nConfig {

    /** Idioma por defecto del sistema. */
    public static final Locale ESPANOL = Locale.forLanguageTag("es");

    /** Idioma alternativo. */
    public static final Locale INGLES = Locale.ENGLISH;

    /**
     * Resuelve el idioma de cada petición a partir de la cabecera {@code Accept-Language}.
     *
     * @return resolutor que admite es y en, con es por defecto
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolutor = new AcceptHeaderLocaleResolver();
        resolutor.setSupportedLocales(List.of(ESPANOL, INGLES));
        resolutor.setDefaultLocale(ESPANOL);
        return resolutor;
    }

    /**
     * Hace que Bean Validation use las mismas claves de mensajes que el resto de la aplicación,
     * por ejemplo {@code @NotBlank(message = "{producto.nombre.obligatorio}")}.
     *
     * @param mensajes fuente de mensajes configurada con {@code spring.messages.basename}
     * @return validador que traduce según el idioma de la petición
     */
    @Bean
    public LocalValidatorFactoryBean getValidator(MessageSource mensajes) {
        LocalValidatorFactoryBean validador = new LocalValidatorFactoryBean();
        validador.setValidationMessageSource(mensajes);
        return validador;
    }
}
