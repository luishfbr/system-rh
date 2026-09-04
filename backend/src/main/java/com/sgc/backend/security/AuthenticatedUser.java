package com.sgc.backend.security;

import com.sgc.backend.domain.entity.User;
import com.sgc.backend.domain.enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adaptador entre a entidade {@link User} e o contrato {@code UserDetails} do
 * Spring Security.
 *
 * <p>Existe para nao acoplar a entidade JPA ao framework de seguranca: a
 * {@code User} continua sendo apenas um registro de banco, e toda a traducao
 * ("qual e o username?", "quais autoridades?") fica isolada aqui.
 *
 * @param id       chave primaria, embarcada no token para evitar uma consulta extra
 * @param email    usado como {@code username} e como {@code subject} do JWT
 * @param fullName exibido na interface
 */
public record AuthenticatedUser(
        Long id,
        String email,
        String fullName,
        String passwordHash,
        Role role,
        boolean active
) implements UserDetails {

    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPasswordHash(),
                user.getRole(),
                user.isActive());
    }

    /**
     * O Spring Security compara autoridades por string. {@code hasRole("ADMIN")}
     * procura a autoridade {@code ROLE_ADMIN} -- dai o prefixo em
     * {@link Role#getAuthority()}.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.getAuthority()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    /** Usuario inativado (RF01-RF03) nao consegue autenticar. */
    @Override
    public boolean isEnabled() {
        return active;
    }
}
