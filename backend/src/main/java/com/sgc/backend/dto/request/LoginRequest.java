package com.sgc.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Credenciais enviadas ao endpoint de login. */
public record LoginRequest(

        @Schema(example = "admin@sgc.com")
        @NotBlank(message = "email e obrigatorio")
        @Email(message = "email invalido")
        String email,

        @Schema(example = "Admin@123")
        @NotBlank(message = "senha e obrigatoria")
        String password
) {
}
