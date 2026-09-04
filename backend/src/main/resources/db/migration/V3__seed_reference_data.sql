-- =============================================================================
-- V3 - Dados de referencia
--
-- Areas, cargos e unidades minimos para que ja seja possivel cadastrar um
-- colaborador logo apos subir o projeto. Sao listas administraveis: em producao
-- o cliente ajusta os valores, estes servem de ponto de partida.
-- =============================================================================

INSERT INTO departments (name, active, created_at, created_by) VALUES
    ('Gestao de Pessoas',   TRUE, NOW(), 'flyway'),
    ('Tecnologia',          TRUE, NOW(), 'flyway'),
    ('Financeiro',          TRUE, NOW(), 'flyway'),
    ('Comercial',           TRUE, NOW(), 'flyway'),
    ('Operacoes',           TRUE, NOW(), 'flyway');

INSERT INTO job_positions (title, active, created_at, created_by) VALUES
    ('Analista de RH',              TRUE, NOW(), 'flyway'),
    ('Desenvolvedor',               TRUE, NOW(), 'flyway'),
    ('Analista Financeiro',         TRUE, NOW(), 'flyway'),
    ('Executivo de Contas',         TRUE, NOW(), 'flyway'),
    ('Assistente Administrativo',   TRUE, NOW(), 'flyway'),
    ('Gerente',                     TRUE, NOW(), 'flyway');

INSERT INTO units (name, active, created_at, created_by) VALUES
    ('Matriz',            TRUE, NOW(), 'flyway'),
    ('Filial Centro',     TRUE, NOW(), 'flyway'),
    ('Filial Zona Sul',   TRUE, NOW(), 'flyway');
