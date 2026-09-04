-- =============================================================================
-- V4 - Separa PESSOA de VINCULO, e cria a timeline (RF08 / RF11)
--
-- Motivacao: um colaborador pode ser readmitido, e cada periodo de contrato tem
-- matricula, cargo, salario e datas proprios. Com uma unica tabela `employees`,
-- registrar o segundo periodo exigiria duplicar todos os dados pessoais -- e
-- nada garantiria que as copias concordassem entre si.
--
--   persons              a pessoa fisica. CPF unico de verdade (RN02).
--     |- addresses       endereco e da pessoa, nao do contrato
--     `- employments     um registro por periodo de contrato
--          `- employment_events   linha do tempo daquele vinculo (RF11)
--
-- A RN07 ("nao admitir CPF ja ativo") deixa de ser so codigo e vira um indice
-- unico parcial: no maximo um vinculo nao-desligado por pessoa.
--
-- ATENCAO: V1 ja foi aplicada em ambientes existentes, entao esta migration
-- avanca o schema (cria, copia, remove) em vez de reescrever a V1.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- 1) persons -- dados da pessoa fisica
-- -----------------------------------------------------------------------------
CREATE TABLE persons (
    id                        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    cpf                       VARCHAR(11)  NOT NULL,
    rg                        VARCHAR(20),
    full_name                 VARCHAR(150) NOT NULL,
    birth_date                DATE         NOT NULL,
    personal_email            VARCHAR(255),
    gender                    VARCHAR(30),
    phone                     VARCHAR(20),
    dependents_count          INTEGER      NOT NULL DEFAULT 0,
    driver_license_categories VARCHAR(10),

    created_at                TIMESTAMPTZ  NOT NULL,
    updated_at                TIMESTAMPTZ,
    created_by                VARCHAR(255),
    updated_by                VARCHAR(255),
    version                   BIGINT       NOT NULL DEFAULT 0,

    -- RN02: agora aplicada a PESSOA. Uma pessoa existe uma unica vez no sistema,
    -- por mais vinculos que tenha tido.
    CONSTRAINT uk_persons_cpf           UNIQUE (cpf),
    CONSTRAINT ck_persons_cpf_digits    CHECK (cpf ~ '^[0-9]{11}$'),
    CONSTRAINT ck_persons_gender        CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'NON_BINARY', 'OTHER', 'UNDISCLOSED')),
    CONSTRAINT ck_persons_dependents    CHECK (dependents_count >= 0)
);

CREATE INDEX idx_persons_full_name ON persons (LOWER(full_name));


-- -----------------------------------------------------------------------------
-- 2) employments -- um registro por periodo de contrato
-- -----------------------------------------------------------------------------
CREATE TABLE employments (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    person_id           BIGINT       NOT NULL,

    registration_number VARCHAR(20),                 -- matricula
    corporate_email     VARCHAR(255),
    hire_date           DATE         NOT NULL,       -- admissao deste periodo
    termination_date    DATE,
    termination_reason  VARCHAR(30),
    termination_notes   TEXT,
    status              VARCHAR(20)  NOT NULL,

    salary              NUMERIC(12,2),
    bonus               NUMERIC(12,2),               -- gratificacao
    weekly_hours        NUMERIC(5,2),                -- carga horaria
    grade               VARCHAR(10),
    band                VARCHAR(10),                 -- faixa

    department_id       BIGINT,
    job_position_id     BIGINT,
    unit_id             BIGINT,

    notes               TEXT,                        -- observacoes

    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    version             BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_employments_registration_number UNIQUE (registration_number),
    CONSTRAINT uk_employments_corporate_email     UNIQUE (corporate_email),

    CONSTRAINT ck_employments_status CHECK (status IN ('ACTIVE', 'ON_LEAVE', 'TERMINATED', 'INACTIVE')),
    CONSTRAINT ck_employments_termination_reason CHECK (
        termination_reason IS NULL OR termination_reason IN (
            'RESIGNATION',          -- pedido de demissao
            'DISMISSAL_WITHOUT_CAUSE',
            'DISMISSAL_WITH_CAUSE',
            'CONTRACT_END',         -- fim de contrato / termino de experiencia
            'RETIREMENT',
            'DEATH',
            'OTHER')),
    CONSTRAINT ck_employments_salary CHECK (salary IS NULL OR salary >= 0),
    CONSTRAINT ck_employments_bonus  CHECK (bonus  IS NULL OR bonus  >= 0),

    -- Desligado obrigatoriamente tem data e motivo; a data nunca antecede a admissao.
    CONSTRAINT ck_employments_terminated_has_date
        CHECK (status <> 'TERMINATED' OR termination_date IS NOT NULL),
    CONSTRAINT ck_employments_terminated_has_reason
        CHECK (status <> 'TERMINATED' OR termination_reason IS NOT NULL),
    CONSTRAINT ck_employments_termination_after_hire
        CHECK (termination_date IS NULL OR termination_date >= hire_date),

    CONSTRAINT fk_employments_person       FOREIGN KEY (person_id)       REFERENCES persons (id) ON DELETE CASCADE,
    CONSTRAINT fk_employments_department   FOREIGN KEY (department_id)   REFERENCES departments (id),
    CONSTRAINT fk_employments_job_position FOREIGN KEY (job_position_id) REFERENCES job_positions (id),
    CONSTRAINT fk_employments_unit         FOREIGN KEY (unit_id)         REFERENCES units (id)
);

-- RN07 no banco: uma pessoa pode ter varios vinculos historicos, mas no maximo
-- UM que ainda nao foi desligado. Indice parcial (com WHERE) -- por isso e um
-- CREATE UNIQUE INDEX e nao uma UNIQUE constraint, que nao aceita condicao.
CREATE UNIQUE INDEX uk_employments_one_open_per_person
    ON employments (person_id)
    WHERE status <> 'TERMINATED';

CREATE INDEX idx_employments_person      ON employments (person_id);
CREATE INDEX idx_employments_status      ON employments (status);
CREATE INDEX idx_employments_department  ON employments (department_id);
CREATE INDEX idx_employments_unit        ON employments (unit_id);
CREATE INDEX idx_employments_hire_date   ON employments (hire_date);
CREATE INDEX idx_employments_termination ON employments (termination_date);


-- -----------------------------------------------------------------------------
-- 3) Migra os dados de employees. Como employees.cpf era unico, cada linha
--    antiga vira exatamente uma pessoa e um vinculo.
-- -----------------------------------------------------------------------------
INSERT INTO persons (cpf, rg, full_name, birth_date, personal_email, gender, phone,
                     dependents_count, driver_license_categories,
                     created_at, updated_at, created_by, updated_by)
SELECT cpf, rg, full_name, birth_date, personal_email, gender, phone,
       dependents_count, driver_license_categories,
       created_at, updated_at, created_by, updated_by
FROM employees;

INSERT INTO employments (person_id, registration_number, corporate_email, hire_date,
                         termination_date, termination_reason, status,
                         salary, bonus, weekly_hours, grade, band,
                         department_id, job_position_id, unit_id, notes,
                         created_at, updated_at, created_by, updated_by)
SELECT p.id, e.registration_number, e.corporate_email, e.hire_date,
       e.termination_date,
       -- Registros anteriores a esta migration nao tem motivo informado, mas a
       -- CHECK exige um quando o status e TERMINATED.
       CASE WHEN e.status = 'TERMINATED' THEN 'OTHER' END,
       e.status,
       e.salary, e.bonus, e.weekly_hours, e.grade, e.band,
       e.department_id, e.job_position_id, e.unit_id, e.notes,
       e.created_at, e.updated_at, e.created_by, e.updated_by
FROM employees e
JOIN persons p ON p.cpf = e.cpf;


-- -----------------------------------------------------------------------------
-- 4) O endereco passa a pertencer a pessoa, nao ao contrato
-- -----------------------------------------------------------------------------
ALTER TABLE addresses ADD COLUMN person_id BIGINT;

UPDATE addresses a
   SET person_id = p.id
  FROM employees e
  JOIN persons p ON p.cpf = e.cpf
 WHERE a.employee_id = e.id;

ALTER TABLE addresses DROP CONSTRAINT fk_addresses_employee;
ALTER TABLE addresses DROP CONSTRAINT uk_addresses_employee;
ALTER TABLE addresses DROP COLUMN employee_id;

ALTER TABLE addresses ALTER COLUMN person_id SET NOT NULL;
ALTER TABLE addresses ADD CONSTRAINT uk_addresses_person UNIQUE (person_id);
ALTER TABLE addresses ADD CONSTRAINT fk_addresses_person
    FOREIGN KEY (person_id) REFERENCES persons (id) ON DELETE CASCADE;


-- -----------------------------------------------------------------------------
-- 5) employees cumpriu seu papel
-- -----------------------------------------------------------------------------
DROP TABLE employees;


-- -----------------------------------------------------------------------------
-- 6) employment_events -- a linha do tempo (RF11)
--
-- Uma unica tabela para TODOS os tipos de evento, em vez de uma tabela por tipo.
-- Assim a timeline (RF11) e um SELECT ordenado, e RF08, RF09 e RF10 so precisam
-- inserir aqui -- sem nova migration a cada requisito.
--
-- previous_value / new_value sao JSONB porque cada tipo de evento muda campos
-- diferentes: uma mudanca de salario guarda {"salary": 7500}, uma de cargo
-- guarda {"jobPositionId": 3}. Colunas fixas nao dariam conta sem virar uma
-- tabela cheia de nulos.
-- -----------------------------------------------------------------------------
CREATE TABLE employment_events (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employment_id  BIGINT       NOT NULL,

    event_type     VARCHAR(30)  NOT NULL,
    effective_date DATE         NOT NULL,   -- quando o fato vale (pode ser retroativo)

    previous_value JSONB,
    new_value      JSONB,

    reason         VARCHAR(30),             -- motivo controlado (ex.: RESIGNATION)
    notes          TEXT,                    -- detalhe livre

    performed_by   VARCHAR(255),            -- quem executou
    -- Eventos sao fatos imutaveis: nao tem updated_at nem version, porque nunca
    -- sao editados. Corrigir a historia significa registrar um novo evento.
    created_at     TIMESTAMPTZ  NOT NULL,

    CONSTRAINT ck_employment_events_type CHECK (event_type IN (
        -- RF08
        'HIRE',
        'TERMINATION',
        -- RF06
        'INACTIVATION',
        'ACTIVATION',
        -- RF05
        'PROFILE_UPDATE',
        -- RF09 (a implementar)
        'LEAVE_START',
        'LEAVE_END',
        -- RF10 (a implementar)
        'POSITION_CHANGE',
        'SALARY_CHANGE',
        'DEPARTMENT_CHANGE')),

    CONSTRAINT ck_employment_events_reason CHECK (
        reason IS NULL OR reason IN (
            'RESIGNATION', 'DISMISSAL_WITHOUT_CAUSE', 'DISMISSAL_WITH_CAUSE',
            'CONTRACT_END', 'RETIREMENT', 'DEATH', 'OTHER')),

    CONSTRAINT fk_employment_events_employment
        FOREIGN KEY (employment_id) REFERENCES employments (id) ON DELETE CASCADE
);

-- A consulta da timeline e sempre "eventos deste vinculo, do mais recente para o
-- mais antigo" -- o indice composto atende exatamente esse acesso.
CREATE INDEX idx_employment_events_timeline
    ON employment_events (employment_id, effective_date DESC, id DESC);

CREATE INDEX idx_employment_events_type ON employment_events (event_type);


-- -----------------------------------------------------------------------------
-- 7) Backfill: vinculos que ja existiam precisam do evento de admissao, senao a
--    timeline deles comecaria no vazio.
-- -----------------------------------------------------------------------------
INSERT INTO employment_events (employment_id, event_type, effective_date, performed_by, created_at)
SELECT id, 'HIRE', hire_date, 'flyway', NOW()
FROM employments;

INSERT INTO employment_events (employment_id, event_type, effective_date, reason, performed_by, created_at)
SELECT id, 'TERMINATION', termination_date, termination_reason, 'flyway', NOW()
FROM employments
WHERE termination_date IS NOT NULL;
