package com.sgc.backend.mapper;

import com.sgc.backend.domain.entity.User;
import com.sgc.backend.dto.response.UserResponse;

/**
 * Conversao entre {@link User} e seus DTOs.
 *
 * <p>Escrito a mao, sem MapStruct nem ModelMapper: o codigo abaixo e exatamente
 * o que roda, sem geracao nem reflexao. Em troca da verbosidade, fica evidente
 * que o {@code passwordHash} nunca e copiado para a resposta.
 *
 * <p>Metodos estaticos e construtor privado porque nao ha estado nem dependencia
 * a injetar aqui.
 */
public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
