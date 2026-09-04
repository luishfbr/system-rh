package com.sgc.backend.dto.response;

import com.sgc.backend.domain.enums.Role;

import java.time.Instant;

/**
 * Retorno do login bem-sucedido.
 *
 * <p>Alem do token, devolve o perfil e o nome para que a interface possa montar
 * o menu sem precisar decodificar o JWT do lado do cliente.
 *
 * @param tokenType sempre "Bearer" -- e o prefixo esperado no header Authorization
 */
public record LoginResponse(
        String token,
        String tokenType,
        Instant expiresAt,
        Long userId,
        String email,
        String fullName,
        Role role
) {
    public static final String BEARER = "Bearer";
}
