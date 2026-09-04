package com.sgc.backend.domain.enums;

/**
 * Situacao de uma alteracao de carreira (RF10).
 *
 * <p>E o que distingue uma <b>intencao</b> de um <b>fato</b>: enquanto PENDING, a
 * alteracao pode ser corrigida ou cancelada; depois de APPLIED ela virou evento
 * imutavel na timeline e so pode ser desfeita por outra alteracao.
 */
public enum ChangeStatus {

    /** Lancada, com vigencia futura. Aguarda o job diario. */
    PENDING,

    /** Ja passou a valer: o vinculo foi atualizado e o evento gravado. */
    APPLIED,

    /** Desfeita antes de valer. Fica no historico para nao apagar a decisao. */
    CANCELLED;

    public boolean isPending() {
        return this == PENDING;
    }
}
