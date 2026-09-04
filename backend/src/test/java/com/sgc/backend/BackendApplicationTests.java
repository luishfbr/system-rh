package com.sgc.backend;

import org.junit.jupiter.api.Test;

/**
 * Verifica que o contexto da aplicacao sobe inteiro.
 *
 * <p>Parece trivial, mas e o teste que pega bean faltando, dependencia circular,
 * propriedade obrigatoria ausente e -- principalmente -- divergencia entre as
 * entidades JPA e o schema criado pelo Flyway, ja que o
 * {@code ddl-auto: validate} confere as duas coisas no startup.
 */
class BackendApplicationTests extends AbstractIntegrationTest {

	@Test
	void contextLoads() {
	}

}
