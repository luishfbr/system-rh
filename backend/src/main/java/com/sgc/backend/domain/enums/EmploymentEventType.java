package com.sgc.backend.domain.enums;

/**
 * Tipos de evento da linha do tempo (RF11).
 *
 * <p>Todos os eventos moram numa unica tabela {@code employment_events}. Cada
 * requisito novo (RF09, RF10) apenas passa a inserir com um destes tipos, sem
 * precisar de tabela nem de migration propria.
 */
public enum EmploymentEventType {

    // ---------- RF08: admissao e desligamento ----------
    HIRE,
    TERMINATION,

    // ---------- RF06: inativacao ----------
    INACTIVATION,
    ACTIVATION,

    // ---------- RF05: edicao cadastral ----------
    /**
     * Alteracao de dados cadastrais. Fica num tipo separado porque e o unico
     * evento de alto volume: a timeline permite filtra-lo para que as mudancas
     * de carreira nao se percam no meio das trocas de telefone.
     */
    PROFILE_UPDATE,

    // ---------- RF09: afastamentos (a implementar) ----------
    LEAVE_START,
    LEAVE_END,

    // ---------- RF10: mudancas de carreira (a implementar) ----------
    POSITION_CHANGE,
    SALARY_CHANGE,
    DEPARTMENT_CHANGE;

    /**
     * Eventos que contam a trajetoria do colaborador na empresa.
     *
     * <p>Tudo menos {@link #PROFILE_UPDATE}: correcao de telefone ou de endereco
     * e rastreabilidade, nao marco de carreira.
     */
    public boolean isCareerEvent() {
        return this != PROFILE_UPDATE;
    }
}
