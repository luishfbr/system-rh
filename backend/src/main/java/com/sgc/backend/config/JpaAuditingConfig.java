package com.sgc.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Liga o preenchimento automatico dos campos de auditoria (RN03).
 *
 * <p>{@code @EnableJpaAuditing} ativa o {@code AuditingEntityListener} declarado
 * em {@code BaseEntity}, que grava {@code createdAt}/{@code updatedAt}. As datas
 * o Spring resolve sozinho; ja o <i>autor</i> da alteracao so ele nao tem como
 * saber -- e o {@link #auditorAware()} abaixo que informa quem esta logado.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    /** Valor gravado quando nao ha usuario autenticado (migrations, jobs, testes). */
    private static final String SYSTEM_AUDITOR = "system";

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            boolean anonymous = authentication == null
                    || !authentication.isAuthenticated()
                    || authentication instanceof AnonymousAuthenticationToken;

            return Optional.of(anonymous ? SYSTEM_AUDITOR : authentication.getName());
        };
    }
}
