package com.sgc.backend.domain.entity;

import com.sgc.backend.domain.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Usuario do sistema -- Administrador ou Gestor de Pessoas (RF01-RF03).
 *
 * <p>Nao ha relacionamento com {@link Employee}: sao cadastros independentes.
 * Um administrador de TI tem login sem ser colaborador, e a grande maioria dos
 * colaboradores nunca tera acesso ao sistema.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    /** RN01: unico no sistema. Sempre gravado em minusculas (ver UserService). */
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /** Hash BCrypt -- a senha em texto puro nunca chega a esta classe. */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    /**
     * {@code EnumType.STRING} grava "ADMIN"/"HR_MANAGER" no banco.
     * Nunca usar {@code EnumType.ORDINAL}: a posicao da constante viraria o
     * valor persistido, e reordenar o enum corromperia todos os registros.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    /** Desativa o login sem apagar o registro (preserva a trilha de auditoria). */
    @Column(name = "active", nullable = false)
    private boolean active = true;
}
