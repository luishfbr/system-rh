-- =============================================================================
-- V2 - Usuario administrador inicial
--
-- Sem este registro nao ha como fazer o primeiro login e, portanto, nao ha como
-- criar nenhum outro usuario (o cadastro de gestores exige role ADMIN).
--
--   email....: admin@sgc.com
--   senha....: Admin@123
--
-- O hash abaixo e BCrypt (custo 10). TROQUE ESTA SENHA no primeiro acesso --
-- ela esta publicada no repositorio e serve apenas para desenvolvimento.
-- =============================================================================
INSERT INTO users (email, password_hash, full_name, role, active, created_at, created_by)
VALUES (
    'admin@sgc.com',
    '$2a$10$4gTSU.vjYC0bt5QOsX.yLe/Jwwj7bt/O0s8Ioq23fFHCK3DiNIadK',
    'Administrador do Sistema',
    'ADMIN',
    TRUE,
    NOW(),
    'flyway'
);
