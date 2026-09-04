-- =============================================================================
-- V5 - Alteracoes de cargo, salario e setor (RF10)
--
-- Uma alteracao de carreira quase nunca vale no dia em que foi decidida: uma
-- promocao combinada em setembro passa a valer em outubro. Por isso ela nasce
-- como uma INTENCAO com data de vigencia, e so vira fato quando a data chega.
--
--   employment_changes    intencoes. Mutaveis: podem ser corrigidas ou canceladas
--        |                enquanto estao PENDING.
--        |  job diario, quando effective_date <= hoje
--        v
--   employments           passa a valer (valor corrente)
--   employment_events     o fato imutavel entra na timeline (RN06)
--
-- Sao tabelas separadas de proposito: evento e fato consumado e nunca muda;
-- alteracao programada e intencao e pode ser desfeita. Misturar as duas coisas
-- faria a timeline exibir o que ainda nao aconteceu.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- 1) employment_changes
-- -----------------------------------------------------------------------------
CREATE TABLE employment_changes (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employment_id            BIGINT       NOT NULL,

    effective_date           DATE         NOT NULL,   -- quando passa (ou passou) a valer
    status                   VARCHAR(20)  NOT NULL,
    reason                   VARCHAR(30)  NOT NULL,   -- promocao, merito, dissidio...
    notes                    TEXT,

    -- Valores solicitados. NULL significa "este campo nao muda", e e o que
    -- permite uma unica alteracao mexer so no salario, so no cargo, ou nos tres.
    new_salary               NUMERIC(12,2),
    new_job_position_id      BIGINT,
    new_department_id        BIGINT,

    -- Valores anteriores, capturados NO MOMENTO DA APLICACAO -- nunca na criacao.
    -- Entre lancar a alteracao e ela valer, o salario pode ter mudado por outra
    -- via; congelar o "antes" na criacao registraria uma historia falsa.
    previous_salary          NUMERIC(12,2),
    previous_job_position_id BIGINT,
    previous_department_id   BIGINT,

    applied_at               TIMESTAMPTZ,
    cancelled_at             TIMESTAMPTZ,
    cancellation_reason      TEXT,

    created_at               TIMESTAMPTZ  NOT NULL,
    updated_at               TIMESTAMPTZ,
    created_by               VARCHAR(255),
    updated_by               VARCHAR(255),
    version                  BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT ck_employment_changes_status CHECK (status IN ('PENDING', 'APPLIED', 'CANCELLED')),

    CONSTRAINT ck_employment_changes_reason CHECK (reason IN (
        'PROMOTION',            -- promocao
        'MERIT_INCREASE',       -- aumento por merito
        'COLLECTIVE_AGREEMENT', -- dissidio / acordo coletivo
        'TRANSFER',             -- transferencia de setor
        'RESTRUCTURE',          -- reestruturacao organizacional
        'DEMOTION',             -- rebaixamento
        'ROLE_CHANGE',          -- mudanca de funcao sem promocao
        'OTHER')),

    -- Uma alteracao que nao altera nada nao deveria existir.
    CONSTRAINT ck_employment_changes_has_target CHECK (
        new_salary IS NOT NULL
        OR new_job_position_id IS NOT NULL
        OR new_department_id IS NOT NULL),

    -- Status e carimbos de tempo andam juntos.
    CONSTRAINT ck_employment_changes_applied_at CHECK (
        status <> 'APPLIED' OR applied_at IS NOT NULL),
    CONSTRAINT ck_employment_changes_cancelled_at CHECK (
        status <> 'CANCELLED' OR cancelled_at IS NOT NULL),

    CONSTRAINT ck_employment_changes_salary CHECK (new_salary IS NULL OR new_salary >= 0),

    CONSTRAINT fk_employment_changes_employment
        FOREIGN KEY (employment_id) REFERENCES employments (id) ON DELETE CASCADE,
    CONSTRAINT fk_employment_changes_new_job_position
        FOREIGN KEY (new_job_position_id) REFERENCES job_positions (id),
    CONSTRAINT fk_employment_changes_new_department
        FOREIGN KEY (new_department_id) REFERENCES departments (id),
    CONSTRAINT fk_employment_changes_prev_job_position
        FOREIGN KEY (previous_job_position_id) REFERENCES job_positions (id),
    CONSTRAINT fk_employment_changes_prev_department
        FOREIGN KEY (previous_department_id) REFERENCES departments (id)
);

-- O job diario pergunta sempre a mesma coisa: "quais pendentes ja venceram?".
-- Indice parcial, porque so as PENDING interessam a essa consulta -- as aplicadas
-- e canceladas, que serao a maioria com o tempo, ficam de fora do indice.
CREATE INDEX idx_employment_changes_due
    ON employment_changes (effective_date)
    WHERE status = 'PENDING';

CREATE INDEX idx_employment_changes_employment
    ON employment_changes (employment_id, effective_date DESC);

CREATE INDEX idx_employment_changes_status ON employment_changes (status);


-- -----------------------------------------------------------------------------
-- 2) A coluna reason de employment_events precisa aceitar tambem os motivos de
--    alteracao de carreira.
--
--    employment_events e uma tabela polimorfica: o vocabulario valido de reason
--    depende do event_type. Um TERMINATION usa motivos de desligamento; um
--    SALARY_CHANGE usa motivos de alteracao. A CHECK passa a ser a uniao dos
--    dois conjuntos, e no Java a coluna vira String justamente porque nao ha um
--    unico enum que a descreva.
-- -----------------------------------------------------------------------------
ALTER TABLE employment_events DROP CONSTRAINT ck_employment_events_reason;

ALTER TABLE employment_events ADD CONSTRAINT ck_employment_events_reason CHECK (
    reason IS NULL OR reason IN (
        -- motivos de desligamento (RF08)
        'RESIGNATION', 'DISMISSAL_WITHOUT_CAUSE', 'DISMISSAL_WITH_CAUSE',
        'CONTRACT_END', 'RETIREMENT', 'DEATH',
        -- motivos de alteracao de carreira (RF10)
        'PROMOTION', 'MERIT_INCREASE', 'COLLECTIVE_AGREEMENT', 'TRANSFER',
        'RESTRUCTURE', 'DEMOTION', 'ROLE_CHANGE',
        -- comum aos dois
        'OTHER'));


-- -----------------------------------------------------------------------------
-- 3) Rastreabilidade: o evento aponta para a alteracao que o originou.
--    Permite ir da linha do tempo ate o lancamento que a produziu -- quem
--    solicitou, quando, com que observacao.
-- -----------------------------------------------------------------------------
ALTER TABLE employment_events ADD COLUMN employment_change_id BIGINT;

ALTER TABLE employment_events ADD CONSTRAINT fk_employment_events_change
    FOREIGN KEY (employment_change_id) REFERENCES employment_changes (id) ON DELETE SET NULL;
