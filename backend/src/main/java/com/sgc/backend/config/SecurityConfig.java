package com.sgc.backend.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.sgc.backend.security.JwtService;
import com.sgc.backend.security.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

// Spring Boot 4 usa Jackson 3, cujo pacote raiz passou de
// com.fasterxml.jackson para tools.jackson. O ObjectMapper do Jackson 2
// ainda aparece no classpath (dependencia do springdoc), mas nao e um bean.
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Configuracao central do Spring Security (RNF01, RNF02).
 *
 * <p><b>Como uma requisicao autenticada flui:</b>
 * <ol>
 *   <li>o cliente envia {@code Authorization: Bearer <token>};</li>
 *   <li>o filtro do {@code oauth2ResourceServer} extrai o token e chama o
 *       {@link #jwtDecoder(SgcProperties)}, que confere assinatura e validade;</li>
 *   <li>o {@link #jwtAuthenticationConverter()} le a claim {@code role} e a
 *       transforma na autoridade {@code ROLE_...};</li>
 *   <li>o resultado vai para o {@code SecurityContext}, onde {@code @PreAuthorize}
 *       e o {@code AuditorAware} conseguem le-lo.</li>
 * </ol>
 *
 * <p>Nao ha filtro JWT escrito a mao: o {@code oauth2ResourceServer} do proprio
 * Spring Security ja faz esse trabalho, com tratamento de erro e casos de borda
 * que uma implementacao caseira normalmente esquece.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** Rotas liberadas sem autenticacao. */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/login",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter,
                                                   ObjectMapper objectMapper) throws Exception {
        http
                // CSRF protege contra envio automatico de cookies pelo navegador.
                // Esta API nao usa cookies de sessao -- a credencial e um header
                // Bearer, que o navegador nao anexa sozinho -- entao nao se aplica.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // STATELESS: nenhuma HttpSession e criada. Cada requisicao se
                // autentica sozinha pelo token.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        // Token ausente/invalido: a falha ocorre dentro da cadeia de
                        // filtros, antes do DispatcherServlet, entao o
                        // GlobalExceptionHandler nao e alcancado. Estes dois handlers
                        // garantem que a resposta continue no formato ProblemDetail.
                        .authenticationEntryPoint((request, response, ex) ->
                                writeProblem(response, objectMapper, HttpStatus.UNAUTHORIZED,
                                        "Nao autenticado",
                                        "Token ausente, expirado ou invalido.",
                                        "unauthenticated"))
                        .accessDeniedHandler((request, response, ex) ->
                                writeProblem(response, objectMapper, HttpStatus.FORBIDDEN,
                                        "Acesso negado",
                                        "Seu perfil nao possui permissao para executar esta operacao.",
                                        "access-denied")));

        return http.build();
    }

    /**
     * BCrypt aplica um "fator de custo" proposital: verificar uma senha leva
     * dezenas de milissegundos, o que torna ataque de forca bruta inviavel.
     * Cada hash ja embute o proprio salt, entao senhas iguais geram hashes
     * diferentes.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * Monta a autenticacao por usuario e senha usada no endpoint de login.
     * O {@code DaoAuthenticationProvider} busca o usuario pelo
     * {@code UserDetailsService} e compara a senha usando o {@code PasswordEncoder}.
     */
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsServiceImpl userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    // ------------------------------------------------------------------
    // JWT: a mesma chave simetrica assina (encoder) e verifica (decoder).
    // ------------------------------------------------------------------

    @Bean
    public JwtEncoder jwtEncoder(SgcProperties properties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(properties)));
    }

    @Bean
    public JwtDecoder jwtDecoder(SgcProperties properties) {
        return NimbusJwtDecoder.withSecretKey(secretKey(properties))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    private SecretKey secretKey(SgcProperties properties) {
        return new SecretKeySpec(properties.jwt().secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /**
     * Por padrao o resource server procura autoridades na claim {@code scope}, no
     * formato OAuth2. Nosso token guarda o perfil em {@code role}, com um unico
     * valor, e o Spring espera o prefixo {@code ROLE_} para {@code hasRole(...)}.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName(JwtService.CLAIM_ROLE);
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Liberado para o frontend em desenvolvimento; restringir por ambiente
        // quando o dominio de producao existir.
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /** Escreve um ProblemDetail direto no response, fora do fluxo do DispatcherServlet. */
    private static void writeProblem(HttpServletResponse response,
                                     ObjectMapper objectMapper,
                                     HttpStatus status,
                                     String title,
                                     String detail,
                                     String code) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://sgc.local/problems/" + code));
        problem.setProperty("timestamp", Instant.now());

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
