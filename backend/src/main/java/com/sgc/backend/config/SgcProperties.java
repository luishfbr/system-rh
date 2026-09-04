package com.sgc.backend.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuracoes proprias da aplicacao, lidas do prefixo {@code sgc} no
 * application.yaml.
 *
 * <p>Usar {@code @ConfigurationProperties} em vez de varios {@code @Value}
 * espalhados da tipagem forte (Duration em vez de long), agrupa o que e
 * relacionado e -- com {@code @Validated} -- faz a aplicacao <b>falhar no
 * startup</b> se a configuracao estiver invalida, em vez de quebrar no primeiro
 * login em producao.
 *
 * @param jwt parametros de emissao e validacao do token
 */
@Validated
@ConfigurationProperties(prefix = "sgc")
public record SgcProperties(@Valid @NotNull Jwt jwt) {

    /**
     * @param secret     chave de assinatura HS256. Precisa de no minimo 32 bytes
     *                   (256 bits); chaves menores sao rejeitadas pela Nimbus.
     * @param expiration validade do token, em formato ISO-8601 ({@code PT8H})
     * @param issuer     identificador gravado na claim {@code iss}
     */
    public record Jwt(
            @NotBlank String secret,
            @NotNull Duration expiration,
            @NotBlank String issuer
    ) {
        public Jwt {
            if (secret != null && secret.getBytes().length < 32) {
                throw new IllegalArgumentException(
                        "sgc.jwt.secret precisa ter no minimo 32 bytes para HS256. "
                                + "Gere uma chave com: openssl rand -base64 48");
            }
        }
    }
}
