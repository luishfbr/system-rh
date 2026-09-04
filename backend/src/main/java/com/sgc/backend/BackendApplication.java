package com.sgc.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Ponto de entrada da aplicacao.
 *
 * <p>{@code @ConfigurationPropertiesScan} registra as classes anotadas com
 * {@code @ConfigurationProperties} (aqui, {@code SgcProperties}) como beans.
 * Sem ele seria preciso listar cada uma em {@code @EnableConfigurationProperties}.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
