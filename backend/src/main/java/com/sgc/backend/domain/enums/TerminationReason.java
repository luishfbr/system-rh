package com.sgc.backend.domain.enums;

/**
 * Motivo do desligamento (RF08).
 *
 * <p>Lista fechada de proposito: e o que permite agrupar o relatorio de
 * desligamentos do RF07. O detalhe em texto livre vai no campo
 * {@code terminationNotes}, ao lado.
 *
 * <p>Os nomes das constantes estao gravados na CHECK constraint de
 * {@code employments.termination_reason} -- renomear exige migration.
 */
public enum TerminationReason {

    /** Pedido de demissao (iniciativa do colaborador). */
    RESIGNATION,

    /** Demissao sem justa causa (iniciativa da empresa). */
    DISMISSAL_WITHOUT_CAUSE,

    /** Demissao por justa causa. */
    DISMISSAL_WITH_CAUSE,

    /** Fim de contrato ou termino do periodo de experiencia. */
    CONTRACT_END,

    RETIREMENT,

    DEATH,

    /** Casos fora da curva -- detalhar em {@code terminationNotes}. */
    OTHER
}
