package com.sgc.backend.domain.enums;

/**
 * Situacao do colaborador na empresa.
 *
 * <p>Mantido como coluna em {@code employees} (e nao derivado do historico de
 * eventos) para que as regras de negocio e os filtros de listagem sejam uma
 * leitura direta, sem reconstruir estado a cada consulta.
 */
public enum EmploymentStatus {

    /** Trabalhando normalmente. */
    ACTIVE,

    /** Afastado temporariamente (RF09) -- continua sendo funcionario. */
    ON_LEAVE,

    /** Desligado da empresa (RF08). Nao aceita alteracoes de cargo/salario (RN05). */
    TERMINATED,

    /** Inativado no sistema (RF06) -- registro preservado, fora das listagens padrao. */
    INACTIVE;

    /**
     * Regra RN05: um colaborador desligado nao pode receber novas alteracoes de
     * cargo, salario ou setor -- apenas reativacao.
     */
    public boolean acceptsCareerChanges() {
        return this == ACTIVE || this == ON_LEAVE;
    }
}
