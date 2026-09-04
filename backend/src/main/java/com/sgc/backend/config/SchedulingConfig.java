package com.sgc.backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita a execucao de tarefas agendadas (RF10).
 *
 * <p>Sem {@code @EnableScheduling}, as anotacoes {@code @Scheduled} sao
 * simplesmente ignoradas -- o metodo existe, compila e nunca roda.
 *
 * <p>O {@code @ConditionalOnProperty} permite desligar o agendamento nos testes:
 * um job disparando no meio de um teste de integracao aplicaria alteracoes que o
 * teste esperava encontrar pendentes.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "sgc.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
