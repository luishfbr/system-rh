package com.sgc.backend.controller;

import com.sgc.backend.dto.request.LoginRequest;
import com.sgc.backend.dto.response.ApiResponse;
import com.sgc.backend.dto.response.LoginResponse;
import com.sgc.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Autenticacao (RNF01). */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticacao", description = "Login e emissao de token")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * {@code @SecurityRequirements} vazio remove o cadeado deste endpoint no
     * Swagger -- e justamente aqui que o token e obtido.
     */
    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Autentica e devolve o token JWT",
            description = "Use o token retornado no header Authorization: Bearer <token>.")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }
}
