# SGC — Sistema de Gerenciamento de Colaboradores

API REST para centralizar informações pessoais e profissionais de colaboradores,
substituindo o controle manual em planilhas descrito em [`docs/requirements.md`](docs/requirements.md).

> **Estado atual:** implementados **RF01–RF06**, **RF08** e **RF10** (usuários,
> cadastro, admissão, desligamento, readmissão e alterações de carreira com data
> de vigência), com **RF11 parcial** (timeline consultável) e a base do
> **RF07** ("Alterações em andamento" já é um endpoint).
> Pendente: **RF09** (afastamentos) e os demais relatórios do RF07.

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 26 |
| Framework | Spring Boot 4.0.8 (Spring Framework 7, Spring Security 7) |
| Persistência | Spring Data JPA + Hibernate 7.2 |
| Banco | PostgreSQL 16 |
| Migrations | Flyway 11 (SQL versionado) |
| Autenticação | JWT HS256 via Spring Security OAuth2 Resource Server |
| Documentação | springdoc-openapi 3.0.3 (Swagger UI) |
| Testes | JUnit 6, Mockito, AssertJ, Testcontainers 2.0 |

---

## Como executar

### Pré-requisitos
- Docker (com Docker Compose)
- JDK 26 — para rodar pela IDE. Pelo Docker não é necessário.

### Opção 1 — banco no Docker, aplicação na IDE (recomendado no dia a dia)

```bash
docker compose up -d          # sobe postgres + pgadmin
cd backend && ./mvnw spring-boot:run
```

### Opção 2 — tudo containerizado

```bash
docker compose --profile full up -d --build
```

### Endereços

| Serviço | URL |
|---|---|
| API | http://localhost:8090 |
| Swagger UI | http://localhost:8090/swagger-ui.html |
| OpenAPI JSON | http://localhost:8090/v3/api-docs |
| Health check | http://localhost:8090/actuator/health |
| pgAdmin | http://localhost:8080 (`admin@sgc.com` / `admin`) |

### Credenciais iniciais

Criadas pela migration `V2__seed_admin_user.sql`:

```
email: admin@sgc.com
senha: Admin@123
```

> ⚠️ Senha de desenvolvimento, versionada no repositório. **Troque no primeiro acesso.**

### Configuração

Sem `.env`, os defaults fazem o projeto subir out-of-the-box. Para customizar,
copie `.env.example` para `.env`. Em produção, obrigatoriamente sobrescreva:

```bash
JWT_SECRET=$(openssl rand -base64 48)   # HS256 exige no mínimo 32 bytes
DB_PASSWORD=...
```

---

## Primeiros passos com a API

```bash
# 1. autenticar
TOKEN=$(curl -s -X POST localhost:8090/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@sgc.com","password":"Admin@123"}' \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['token'])")

# 2. cadastrar um colaborador
curl -X POST localhost:8090/api/v1/employees \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"cpf":"111.444.777-35","fullName":"Maria Souza",
       "birthDate":"1992-03-14","hireDate":"2024-01-15","salary":7500.50}'

# 3. listar com filtros
curl "localhost:8090/api/v1/employees?name=maria&status=ACTIVE" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Endpoints

| Método | Rota | Perfil | Requisito |
|---|---|---|---|
| POST | `/api/v1/auth/login` | público | RNF01 |
| POST | `/api/v1/users` | ADMIN | RF01 |
| PUT | `/api/v1/users/{id}` | ADMIN | RF02 |
| DELETE | `/api/v1/users/{id}` | ADMIN | RF03 |
| GET | `/api/v1/users` · `/{id}` | ADMIN | — |
| POST | `/api/v1/employees` | ADMIN, HR_MANAGER | RF04, RF08 |
| PATCH | `/api/v1/employees/{id}/terminate` | ADMIN, HR_MANAGER | **RF08** |
| PUT | `/api/v1/employees/{id}` | ADMIN, HR_MANAGER | RF05 |
| PATCH | `/api/v1/employees/{id}/inactivate` | ADMIN, HR_MANAGER | RF06 |
| PATCH | `/api/v1/employees/{id}/activate` | ADMIN, HR_MANAGER | RF06 |
| DELETE | `/api/v1/employees/{id}` | **ADMIN** | RN04 |
| GET | `/api/v1/employees` (paginado, filtros) | ADMIN, HR_MANAGER | — |
| GET | `/api/v1/employees/{id}` · `/by-cpf/{cpf}` | ADMIN, HR_MANAGER | — |
| GET | `/api/v1/employees/{id}/timeline` | ADMIN, HR_MANAGER | **RF11** (parcial) |
| POST | `/api/v1/employees/{id}/career-changes` | ADMIN, HR_MANAGER | **RF10** |
| GET | `/api/v1/employees/{id}/career-changes` | ADMIN, HR_MANAGER | RF10 |
| PATCH | `/api/v1/career-changes/{id}/cancel` | ADMIN, HR_MANAGER | RF10 |
| GET | `/api/v1/career-changes?status=PENDING` | ADMIN, HR_MANAGER | **RF07** (alterações em andamento) |
| GET | `/api/v1/persons/by-cpf/{cpf}` · `/{id}` | ADMIN, HR_MANAGER | — |
| GET | `/api/v1/reference/{departments,job-positions,units}` | ADMIN, HR_MANAGER | — |

---

## Estrutura do código

Organização por **camada técnica**:

```
com.sgc.backend
├── config/          SecurityConfig, OpenApiConfig, JpaAuditingConfig, SgcProperties
├── controller/      endpoints REST
├── service/         regras de negócio (RN01–RN07)
├── repository/      Spring Data + Specifications (filtros dinâmicos)
├── domain/
│   ├── entity/      Person, Employment, EmploymentEvent, Address, User…
│   └── enums/       Role, EmploymentStatus, Gender, TerminationReason,
│                    EmploymentEventType
├── dto/
│   ├── request/     entrada (records + Bean Validation)
│   └── response/    saída (records; ApiResponse é o envelope de sucesso)
├── mapper/          conversão entidade ↔ DTO, escrita à mão
├── security/        JwtService, AuthenticatedUser, UserDetailsServiceImpl
├── exception/       exceções de domínio + GlobalExceptionHandler
└── validation/      @Cpf e seu ConstraintValidator
```

---

## Decisões de arquitetura

**Flyway com SQL versionado, `ddl-auto: validate`.**
O Hibernate nunca altera o banco — apenas confere no boot se as entidades batem com
o schema. Toda mudança estrutural passa por um arquivo `V*.sql` revisável e
reproduzível em qualquer ambiente.

**Erros em ProblemDetail (RFC 9457), sucesso em `ApiResponse`.**
Erros usam o padrão nativo do Spring (`application/problem+json`), com uma URI de
`type` estável por regra de negócio — o frontend reage ao código, não ao texto da
mensagem. O envelope `ApiResponse` cobre só o caminho feliz; ele não tem campo
`success` porque quem indica falha é o status HTTP.

**Pessoa e vínculo são tabelas separadas.**
Um colaborador pode ser readmitido, e cada período de contrato tem matrícula,
cargo, salário e datas próprios. Com uma tabela só, o segundo período exigiria
duplicar todos os dados pessoais — e nada garantiria que as cópias concordassem.

```
persons              a pessoa física. CPF único (RN02).
  ├── addresses      o endereço é da pessoa, não do contrato
  └── employments    um registro por período de contrato
        └── employment_events    linha do tempo daquele vínculo
```

Na API, `/api/v1/employees` é o **vínculo** — é o que o gestor quer ver ao listar
colaboradores. Consequência elegante: **readmissão não tem endpoint próprio**, é
um `POST /employees` com o mesmo CPF. A operação só é recusada quando já existe
vínculo em aberto, que é exatamente o texto da RN07.

**A RN07 vive no banco, não só no código.**
`CREATE UNIQUE INDEX ... ON employments (person_id) WHERE status <> 'TERMINATED'` —
um índice único *parcial* garante no máximo um vínculo em aberto por pessoa. A
checagem no service existe apenas para produzir uma mensagem legível; quem
realmente impede a corrida entre duas requisições simultâneas é o banco.

**`users` e `employments` sem FK entre si.**
Um administrador de TI tem login sem ser colaborador; a maioria dos colaboradores
nunca terá acesso ao sistema. São ciclos de vida independentes.

**Uma única tabela polimórfica para a timeline.**
`employment_events` guarda todos os tipos de evento, com `previous_value` e
`new_value` em JSONB porque cada tipo altera campos diferentes. Assim o RF11 é um
`SELECT` ordenado, e RF09/RF10 só precisarão inserir — sem migration nova. Eventos
são imutáveis: a entidade não tem `updated_at` nem `@Version`, e corrigir a
história significa registrar um evento novo.

**Status do vínculo é coluna, não derivado de eventos.**
A RN05 vira uma verificação direta em vez de reconstruir o estado a cada leitura.

**Alteração de carreira é intenção antes de ser fato.**
Uma promoção quase nunca vale no dia em que foi decidida. Por isso ela nasce em
`employment_changes` com data de vigência: no futuro fica `PENDING` (é essa fila
que o relatório "Alterações em andamento" mostra), no passado ou hoje aplica na
hora. Tabela separada de `employment_events` porque intenção pode ser cancelada e
evento não — misturá-las faria a timeline exibir o que ainda não aconteceu.

O estado anterior é **congelado no momento da aplicação**, nunca na criação:
entre lançar a alteração e ela valer, o salário pode ter mudado por outra via, e
gravar o "antes" cedo demais registraria uma história falsa.

**`PUT /employees/{id}` não altera salário, cargo nem área.**
Esses três campos passam obrigatoriamente pelo RF10, que exige motivo e vigência.
Dois caminhos para a mesma mudança deixariam a RN06 furada — daria para alterar
um salário sem dizer por quê e sem registrar na timeline.

**JWT HS256 com a infraestrutura nativa do Spring Security.**
`NimbusJwtEncoder`/`NimbusJwtDecoder` em vez de jjwt, e o `oauth2ResourceServer`
no lugar de um `OncePerRequestFilter` caseiro — menos código próprio e todos os
casos de borda já tratados. Como emissor e validador são a mesma aplicação, a
chave simétrica basta; RS256 é o caminho quando outro serviço precisar validar
tokens sem poder emiti-los.

**Mapeamento entidade ↔ DTO escrito à mão.**
Sem MapStruct nem ModelMapper: o código que roda é o que está escrito. É onde
mora o mascaramento do CPF e a garantia de que `passwordHash` nunca sai da API.

**Specifications para os filtros da listagem.**
Quatro filtros opcionais dariam dezesseis métodos no repository. Cada filtro é
escrito uma vez e combinado sob demanda, com `fetch join` para evitar N+1.

**Testcontainers com Postgres real.**
As migrations usam `GENERATED ALWAYS AS IDENTITY`, CHECK com regex e índice
funcional `LOWER(full_name)` — o H2 não reproduz isso. Um teste que passa no H2 e
quebra em produção é pior do que não ter teste.

---

## Regras de negócio implementadas

| Regra | Onde | Comportamento |
|---|---|---|
| RN01 | `UserService` | Email único; normalizado para minúsculas |
| RN02 | `persons.cpf` unique | CPF identifica a **pessoa**, que existe uma única vez |
| RN03 | `BaseEntity` + `JpaAuditingConfig` | `created_by`/`updated_by` do usuário autenticado |
| RN04 | `EmployeeController` | Inativação: gestor. Exclusão definitiva: só ADMIN |
| RN05 | `EmploymentService` | Vínculo desligado não aceita edição — só readmissão |
| RN06 | `EmploymentEventRecorder` | Toda mudança de situação, salário, cargo ou setor grava evento |
| RN07 | índice parcial `uk_employments_one_open_per_person` | No máximo um vínculo em aberto por pessoa |

Validação de CPF com dígitos verificadores em `@Cpf` / `CpfValidator`.

---

## Testes

```bash
cd backend
./mvnw test          # requer Docker rodando (Testcontainers)
```

| Teste | Tipo | Cobre |
|---|---|---|
| `CpfValidatorTest` | unitário puro | Algoritmo do CPF |
| `EmploymentServiceTest` | Mockito | Admissão, readmissão, desligamento, RN05, RN07 |
| `EmployeeApiIntegrationTest` | ponta a ponta | Ciclo admitir→desligar→readmitir, timeline, RBAC |
| `CareerChangeApiIntegrationTest` | ponta a ponta | Alteração programada, aplicação pelo job, cancelamento, RN05 |
| `BackendApplicationTests` | smoke | Contexto sobe; entidades batem com o schema |

---

## Banco de dados

### Migrations

| Arquivo | Conteúdo |
|---|---|
| `V1__initial_schema.sql` | `users`, `departments`, `job_positions`, `units`, `employees`, `addresses` |
| `V2__seed_admin_user.sql` | Administrador inicial |
| `V3__seed_reference_data.sql` | Áreas, cargos e unidades de partida |
| `V4__split_person_and_employment.sql` | Separa `persons` de `employments`, cria `employment_events`, migra os dados e remove `employees` |
| `V5__employment_changes.sql` | Cria `employment_changes` (alterações com vigência), amplia os motivos aceitos nos eventos e liga evento → alteração |

### Convenções

- PK: `BIGINT GENERATED ALWAYS AS IDENTITY`
- Enums: `VARCHAR` + `CHECK` (evita `ALTER TYPE` em migration)
- Dinheiro: `NUMERIC(12,2)` — nunca `float`
- Datas de negócio: `DATE`; timestamps técnicos: `TIMESTAMPTZ`
- Toda tabela tem auditoria (`created_at/by`, `updated_at/by`) e `version`

---

## Próximos passos

1. **RF09** — afastamentos, gravando `LEAVE_START` / `LEAVE_END` na timeline
2. **RF11** — completar a timeline com os eventos do RF09 e a visão consolidada
   por pessoa (hoje a timeline é por vínculo)
3. **RF07** — os demais relatórios (desligamentos, admissões, faixa etária) e a
   exportação. Os filtros `hiredFrom`/`hiredUntil`, `termination_reason` e
   `ChangeReason` já foram desenhados para isso
4. **RNF07** — criptografia de CPF/RG via `AttributeConverter`
5. **RN03** — tabela `audit_log` completa, além da auditoria de campos
6. **Lock distribuído** no job do RF10 (ShedLock), antes de rodar mais de uma
   instância da aplicação
7. Refresh token e rotação de credenciais
