package com.sgc.backend.domain.enums;

/**
 * Perfis de acesso ao sistema (RNF02).
 *
 * <p>O nome da constante e gravado literalmente na coluna {@code users.role},
 * que possui uma CHECK constraint com exatamente estes valores. Renomear uma
 * constante aqui exige uma migration correspondente.
 *
 * <p>O Spring Security espera autoridades com o prefixo {@code ROLE_} ao usar
 * {@code hasRole(...)}; a conversao acontece em {@link #getAuthority()}.
 */
public enum Role {

    /** Acesso total, incluindo exclusao definitiva de colaboradores (RN04). */
    ADMIN,

    /** Gestor de Pessoas: opera o cadastro, mas nao exclui definitivamente. */
    HR_MANAGER;

    public String getAuthority() {
        return "ROLE_" + name();
    }
}
