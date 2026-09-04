package com.sgc.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentacao interativa da API (Swagger UI em {@code /swagger-ui.html}).
 *
 * <p>O springdoc varre os controllers sozinho; o que precisa ser declarado aqui
 * e o <b>esquema de seguranca</b>. Sem ele, o Swagger monta as requisicoes sem o
 * header {@code Authorization} e todo endpoint protegido responde 401 -- o botao
 * "Authorize" sequer aparece na tela.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI sgcOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SGC - Sistema de Gerenciamento de Colaboradores")
                        .version("v1")
                        .description("""
                                API de gestao de colaboradores.

                                **Como autenticar:** chame `POST /api/v1/auth/login`, copie o campo
                                `token` da resposta e informe-o no botao **Authorize** acima.
                                """)
                        .contact(new Contact().name("Equipe SGC")))
                // Aplica o esquema a todos os endpoints por padrao.
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Informe apenas o token; o prefixo 'Bearer' e adicionado automaticamente.")));
    }
}
