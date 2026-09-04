package com.sgc.backend.security;

import com.sgc.backend.config.SgcProperties;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Emissao dos tokens JWT.
 *
 * <p>A validacao do token na entrada <b>nao</b> passa por aqui: quem faz isso e o
 * {@code oauth2ResourceServer} configurado em {@code SecurityConfig}, usando o
 * {@code JwtDecoder}. Esta classe cuida apenas do caminho de saida (login).
 *
 * <p>O token e assinado com <b>HS256</b> (chave simetrica). Como quem emite e
 * quem valida sao a mesma aplicacao, um segredo compartilhado basta e evita a
 * gestao de um par de chaves. Se um dia outro servico precisar validar tokens
 * sem poder emiti-los, o caminho e migrar para RS256.
 */
@Service
public class JwtService {

    /** Claim com o perfil do usuario; lido de volta pelo conversor em SecurityConfig. */
    public static final String CLAIM_ROLE = "role";

    /** Claim com o id do usuario, evitando uma consulta ao banco a cada requisicao. */
    public static final String CLAIM_USER_ID = "uid";

    public static final String CLAIM_NAME = "name";

    private final JwtEncoder jwtEncoder;
    private final SgcProperties properties;

    public JwtService(JwtEncoder jwtEncoder, SgcProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    /**
     * Gera o token de acesso para um usuario ja autenticado.
     *
     * @return o token e o instante em que ele expira
     */
    public IssuedToken generate(AuthenticatedUser user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.jwt().expiration());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.email())
                .claim(CLAIM_USER_ID, user.id())
                .claim(CLAIM_ROLE, user.role().name())
                .claim(CLAIM_NAME, user.fullName())
                .build();

        // O JwsHeader precisa ser explicito: o padrao do NimbusJwtEncoder e RS256,
        // que nao combina com a chave simetrica configurada.
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        String tokenValue = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(tokenValue, expiresAt);
    }

    /** Token recem-emitido e seu vencimento. */
    public record IssuedToken(String value, Instant expiresAt) {
    }
}
