package com.sgc.backend.dto.response;

import com.sgc.backend.domain.enums.Role;

import java.time.Instant;

/**
 * Usuario do sistema devolvido pela API.
 *
 * <p>Note a ausencia de qualquer campo de senha: o hash nunca sai da aplicacao.
 * E exatamente por isso que existe um DTO separado da entidade.
 */
public record UserResponse(
        Long id,
        String email,
        String fullName,
        Role role,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
