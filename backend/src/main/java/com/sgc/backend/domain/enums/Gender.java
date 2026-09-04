package com.sgc.backend.domain.enums;

/**
 * Genero informado pelo colaborador.
 *
 * <p>Dado sensivel sob a LGPD (RNF06): o preenchimento e opcional e
 * {@link #UNDISCLOSED} existe para que "nao informar" seja uma escolha
 * explicita, e nao um campo nulo ambiguo.
 */
public enum Gender {
    MALE,
    FEMALE,
    NON_BINARY,
    OTHER,
    UNDISCLOSED
}
