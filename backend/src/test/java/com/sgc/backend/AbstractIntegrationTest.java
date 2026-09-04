package com.sgc.backend;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base para os testes que sobem o contexto completo do Spring.
 *
 * <p>Herdar daqui garante que todos usem a <b>mesma</b> configuracao e, por
 * consequencia, o mesmo contexto em cache -- o container do Postgres sobe uma
 * unica vez para a suite inteira.
 *
 * <p>As migrations Flyway rodam de verdade no container, entao estes testes
 * tambem validam o SQL do {@code db/migration}.
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {
}
