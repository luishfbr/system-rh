-- =============================================================================
-- V1 - Schema inicial do Sistema de Gerenciamento de Colaboradores
--
-- Convencoes adotadas:
--   * PK  : BIGINT GENERATED ALWAYS AS IDENTITY (padrao SQL, substitui SERIAL)
--   * Enum: VARCHAR + CHECK constraint. Enum nativo do Postgres exige ALTER TYPE
--           para incluir valores, o que complica migrations Flyway sem ganho real.
--   * Dinheiro: NUMERIC(12,2) -- nunca usar float para valores monetarios.
--   * Datas de negocio: DATE. Timestamps tecnicos: TIMESTAMPTZ.
--   * Auditoria (created_at/by, updated_at/by) e version em toda tabela,
--     preenchidos pelo JPA Auditing e pelo @Version do Hibernate.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- users -- usuarios do sistema (RF01-RF03)
--
-- Proposital: NAO ha FK para employees. Um administrador de TI pode ter login
-- sem ser colaborador, e a imensa maioria dos colaboradores nunca tera acesso
-- ao sistema. Sao ciclos de vida independentes.
-- -----------------------------------------------------------------------------
CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(150) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ,
    created_by    VARCHAR(255),
    updated_by    VARCHAR(255),
    version       BIGINT       NOT NULL DEFAULT 0,

    -- RN01: cada usuario do sistema deve possuir um email unico.
    -- O email e normalizado para minusculas no service, entao a unique
    -- simples ja garante unicidade case-insensitive.
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role  CHECK (role IN ('ADMIN', 'HR_MANAGER'))
);


-- -----------------------------------------------------------------------------
-- Tabelas de apoio do cadastro profissional.
-- Sao listas controladas: evitam o "texto livre" que hoje gera divergencia
-- entre as planilhas (problema citado nos requisitos).
-- -----------------------------------------------------------------------------

-- "Area de Atuacao"
CREATE TABLE departments (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(120) NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version    BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_departments_name UNIQUE (name)
);

-- "Cargo/Funcao"
CREATE TABLE job_positions (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title      VARCHAR(120) NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version    BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_job_positions_title UNIQUE (title)
);

-- "Agencia/Unidade"
CREATE TABLE units (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(120) NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version    BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_units_name UNIQUE (name)
);


-- -----------------------------------------------------------------------------
-- employees -- o colaborador (RF04-RF06)
--
-- Cargo, salario e setor ficam AQUI como valor corrente. O historico dessas
-- mudancas (RF10/RF11) vai para employment_events numa migration futura.
-- Guardar o valor atual denormalizado deixa listagem e relatorio como um join
-- simples, em vez de reconstruir o estado a partir dos eventos a cada leitura.
-- -----------------------------------------------------------------------------
CREATE TABLE employees (
    id                        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    -- ---------- Cadastro pessoal ----------
    -- Somente digitos, sem pontuacao: a normalizacao acontece no service.
    -- VARCHAR (e nao CHAR) porque o Hibernate mapeia String para varchar e o
    -- ddl-auto=validate acusaria divergencia de tipo com CHAR.
    cpf                       VARCHAR(11)  NOT NULL,
    rg                        VARCHAR(20),
    full_name                 VARCHAR(150) NOT NULL,
    birth_date                DATE         NOT NULL,
    personal_email            VARCHAR(255),
    gender                    VARCHAR(30),
    phone                     VARCHAR(20),
    dependents_count          INTEGER      NOT NULL DEFAULT 0,
    -- Categorias da CNH concatenadas, ex.: 'AB'. Se virar requisito de busca,
    -- normalizar depois em tabela propria.
    driver_license_categories VARCHAR(10),

    -- ---------- Cadastro profissional ----------
    registration_number       VARCHAR(20),                 -- matricula
    corporate_email           VARCHAR(255),
    hire_date                 DATE         NOT NULL,       -- data de admissao
    termination_date          DATE,                        -- data de desligamento
    status                    VARCHAR(20)  NOT NULL,
    salary                    NUMERIC(12,2),
    bonus                     NUMERIC(12,2),               -- gratificacao
    weekly_hours              NUMERIC(5,2),                -- carga horaria
    grade                     VARCHAR(10),
    band                      VARCHAR(10),                 -- faixa

    department_id             BIGINT,
    job_position_id           BIGINT,
    unit_id                   BIGINT,

    notes                     TEXT,                        -- observacoes

    created_at                TIMESTAMPTZ  NOT NULL,
    updated_at                TIMESTAMPTZ,
    created_by                VARCHAR(255),
    updated_by                VARCHAR(255),
    version                   BIGINT       NOT NULL DEFAULT 0,

    -- RN02: cada colaborador deve possuir um CPF unico.
    CONSTRAINT uk_employees_cpf                 UNIQUE (cpf),
    CONSTRAINT uk_employees_registration_number UNIQUE (registration_number),
    CONSTRAINT uk_employees_corporate_email     UNIQUE (corporate_email),

    CONSTRAINT ck_employees_cpf_digits CHECK (cpf ~ '^[0-9]{11}$'),
    CONSTRAINT ck_employees_status     CHECK (status IN ('ACTIVE', 'ON_LEAVE', 'TERMINATED', 'INACTIVE')),
    CONSTRAINT ck_employees_gender     CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'NON_BINARY', 'OTHER', 'UNDISCLOSED')),
    CONSTRAINT ck_employees_dependents CHECK (dependents_count >= 0),
    CONSTRAINT ck_employees_salary     CHECK (salary IS NULL OR salary >= 0),
    CONSTRAINT ck_employees_bonus      CHECK (bonus  IS NULL OR bonus  >= 0),

    -- Um colaborador desligado obrigatoriamente tem data de desligamento,
    -- e ela nunca pode ser anterior a admissao.
    CONSTRAINT ck_employees_terminated_has_date
        CHECK (status <> 'TERMINATED' OR termination_date IS NOT NULL),
    CONSTRAINT ck_employees_termination_after_hire
        CHECK (termination_date IS NULL OR termination_date >= hire_date),

    CONSTRAINT fk_employees_department   FOREIGN KEY (department_id)   REFERENCES departments (id),
    CONSTRAINT fk_employees_job_position FOREIGN KEY (job_position_id) REFERENCES job_positions (id),
    CONSTRAINT fk_employees_unit         FOREIGN KEY (unit_id)         REFERENCES units (id)
);

-- Indices de leitura: status e o filtro mais comum das telas e relatorios;
-- full_name e o campo de busca livre.
CREATE INDEX idx_employees_status        ON employees (status);
CREATE INDEX idx_employees_department    ON employees (department_id);
CREATE INDEX idx_employees_job_position  ON employees (job_position_id);
CREATE INDEX idx_employees_unit          ON employees (unit_id);
CREATE INDEX idx_employees_full_name     ON employees (LOWER(full_name));
CREATE INDEX idx_employees_hire_date     ON employees (hire_date);


-- -----------------------------------------------------------------------------
-- addresses -- endereco do colaborador (1:1)
-- Tabela separada porque o endereco e opcional e tem ciclo proprio; manter os
-- oito campos dentro de employees engordaria a tabela mais consultada do sistema.
-- -----------------------------------------------------------------------------
CREATE TABLE addresses (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id  BIGINT       NOT NULL,

    street       VARCHAR(150) NOT NULL,
    number       VARCHAR(20),
    complement   VARCHAR(100),
    district     VARCHAR(100),
    city         VARCHAR(100) NOT NULL,
    state        VARCHAR(2)   NOT NULL,
    zip_code     VARCHAR(8)   NOT NULL,

    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ,
    created_by   VARCHAR(255),
    updated_by   VARCHAR(255),
    version      BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_addresses_employee UNIQUE (employee_id),
    CONSTRAINT ck_addresses_zip_code CHECK (zip_code ~ '^[0-9]{8}$'),
    CONSTRAINT ck_addresses_state    CHECK (state ~ '^[A-Z]{2}$'),
    -- ON DELETE CASCADE: a exclusao definitiva do colaborador (RN04, restrita a
    -- administradores) leva junto o endereco.
    CONSTRAINT fk_addresses_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE
);
