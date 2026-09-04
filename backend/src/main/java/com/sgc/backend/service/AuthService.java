package com.sgc.backend.service;

import com.sgc.backend.dto.request.LoginRequest;
import com.sgc.backend.dto.response.LoginResponse;
import com.sgc.backend.security.AuthenticatedUser;
import com.sgc.backend.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Autenticacao por email e senha, com emissao do JWT (RNF01).
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * Valida as credenciais e devolve o token de acesso.
     *
     * <p>Quem compara a senha com o hash e o {@code AuthenticationManager};
     * em caso de falha ele lanca {@code BadCredentialsException} (usuario
     * inexistente ou senha errada) ou {@code DisabledException} (usuario
     * inativo), ambas convertidas em ProblemDetail pelo
     * {@code GlobalExceptionHandler}.
     */
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email().toLowerCase(Locale.ROOT),
                        request.password()));

        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        JwtService.IssuedToken token = jwtService.generate(user);

        return new LoginResponse(
                token.value(),
                LoginResponse.BEARER,
                token.expiresAt(),
                user.id(),
                user.email(),
                user.fullName(),
                user.role());
    }
}
