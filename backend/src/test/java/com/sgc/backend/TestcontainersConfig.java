package com.sgc.backend;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Sobe um PostgreSQL real em container para os testes de integracao.
 *
 * <p><b>Por que nao H2:</b> o banco em memoria fala um dialeto diferente. As
 * migrations Flyway usam {@code GENERATED ALWAYS AS IDENTITY}, CHECK com regex
 * ({@code ~ '^[0-9]{11}$'}) e indice funcional {@code LOWER(full_name)} -- coisas
 * que o H2 nao aceita ou interpreta de outro jeito. Um teste que passa no H2 e
 * quebra em producao e pior do que nao ter teste.
 *
 * <p><b>{@code @ServiceConnection}</b> e a peca que dispensa configuracao manual:
 * o Spring Boot le host, porta, usuario e senha do container e injeta no
 * datasource da aplicacao. Sem ela seria preciso um {@code @DynamicPropertySource}
 * repetindo cada propriedade.
 *
 * <p>O container e um bean: o Spring cuida do ciclo de vida e, gracas ao cache de
 * contexto do framework de teste, ele e reaproveitado entre as classes de teste
 * em vez de subir um Postgres novo a cada uma.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    /** Mesma imagem do docker-compose -- testar contra outra versao esconderia incompatibilidades. */
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:16");
    }
}
