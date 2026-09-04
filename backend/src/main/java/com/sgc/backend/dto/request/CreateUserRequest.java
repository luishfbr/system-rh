package com.sgc.backend.dto.request;

import com.sgc.backend.domain.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cadastro de um usuario do sistema (RF01).
 *
 * <p>A senha chega em texto puro e e transformada em hash BCrypt no service --
 * nunca e persistida nem registrada em log.
 */
public record CreateUserRequest(

        @NotBlank(message = "email e obrigatorio")
        @Email(message = "email invalido")
        @Size(max = 255)
        String email,

        @NotBlank(message = "senha e obrigatoria")
        @Size(min = 8, max = 100, message = "a senha deve ter entre 8 e 100 caracteres")
        String password,

        @NotBlank(message = "nome completo e obrigatorio")
        @Size(max = 150)
        String fullName,

        @NotNull(message = "perfil e obrigatorio")
        Role role
) {
}
