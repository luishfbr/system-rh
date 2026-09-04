package com.sgc.backend.domain.enums;

/**
 * Motivo de uma alteracao de cargo, salario ou setor (RF10).
 *
 * <p>Lista fechada, como no desligamento: e o que permite ao relatorio do RF07
 * responder "quantas promocoes tivemos no semestre?" em vez de depender do que
 * cada gestor escreveu no campo livre.
 *
 * <p>Os nomes estao gravados nas CHECK constraints de
 * {@code employment_changes.reason} e {@code employment_events.reason} --
 * renomear exige migration.
 */
public enum ChangeReason {

    /** Promocao: normalmente muda cargo e salario juntos. */
    PROMOTION,

    /** Aumento por merito, sem mudanca de cargo. */
    MERIT_INCREASE,

    /** Dissidio ou acordo coletivo -- reajuste que atinge toda uma categoria. */
    COLLECTIVE_AGREEMENT,

    /** Transferencia de setor ou unidade. */
    TRANSFER,

    /** Reestruturacao organizacional. */
    RESTRUCTURE,

    /** Rebaixamento de cargo. */
    DEMOTION,

    /** Mudanca de funcao sem promocao nem aumento. */
    ROLE_CHANGE,

    OTHER
}
