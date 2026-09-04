package com.sgc.backend;

import com.sgc.backend.repository.EmploymentRepository;
import com.sgc.backend.repository.PersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
// Boot 4 reorganizou os pacotes de teste por modulo: @AutoConfigureMockMvc saiu de
// org.springframework.boot.test.autoconfigure.web.servlet para o modulo webmvc.
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de ponta a ponta: contexto completo, seguranca ativa e Postgres real.
 *
 * <p>Exercita o ciclo de vida inteiro do colaborador -- admitir, editar,
 * desligar, readmitir -- e por isso cobre de uma vez o filtro JWT, o
 * {@code @PreAuthorize}, a validacao, o mapeamento JPA, as migrations Flyway e a
 * gravacao da timeline.
 *
 * <p>O administrador nao e criado aqui: vem da migration
 * {@code V2__seed_admin_user.sql}, que roda no container.
 */
@AutoConfigureMockMvc
class EmployeeApiIntegrationTest extends AbstractIntegrationTest {

    private static final String CPF = "111.444.777-35";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmploymentRepository employmentRepository;

    @Autowired
    private PersonRepository personRepository;

    private String token;

    @BeforeEach
    void autenticar() throws Exception {
        // Cada teste comeca de um estado conhecido. A ordem importa: o vinculo
        // referencia a pessoa.
        employmentRepository.deleteAll();
        personRepository.deleteAll();

        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@sgc.com","password":"Admin@123"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        token = com.jayway.jsonpath.JsonPath.read(response, "$.data.token");
    }

    @Test
    @DisplayName("Sem token, a API responde 401 no formato ProblemDetail")
    void deveExigirAutenticacao() throws Exception {
        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Nao autenticado"));
    }

    @Test
    @DisplayName("RF08: admitir grava o evento HIRE na timeline")
    void admissaoGeraEventoNaTimeline() throws Exception {
        int id = admitir(CPF, "2020-02-10", "MAT-0001", 5000);

        mockMvc.perform(get("/api/v1/employees/{id}/timeline", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].eventType").value("HIRE"))
                // Data efetiva e a admissao, nao "hoje".
                .andExpect(jsonPath("$.data[0].effectiveDate").value("2020-02-10"))
                .andExpect(jsonPath("$.data[0].performedBy").value("admin@sgc.com"));
    }

    @Test
    @DisplayName("RN07: admitir CPF com vinculo em aberto devolve 422")
    void naoAdmiteCpfComVinculoAberto() throws Exception {
        admitir(CPF, "2020-02-10", "MAT-0001", 5000);

        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoAdmissao(CPF, "2026-01-01", "MAT-0002", 6000)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://sgc.local/problems/cpf-already-active"));
    }

    @Test
    @DisplayName("RF08: ciclo completo -- admitir, desligar e readmitir a mesma pessoa")
    void cicloDeAdmissaoDesligamentoEReadmissao() throws Exception {
        int primeiro = admitir(CPF, "2020-02-10", "MAT-0001", 5000);

        // --- desligar ---
        mockMvc.perform(patch("/api/v1/employees/{id}/terminate", primeiro)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"terminationDate":"2023-11-30","reason":"RESIGNATION",
                                 "notes":"Recebeu proposta externa"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("TERMINATED"))
                .andExpect(jsonPath("$.data.terminationReason").value("RESIGNATION"));

        // --- readmitir: o desligamento liberou o CPF ---
        String readmitido = mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoAdmissao(CPF, "2025-03-01", "MAT-0042", 9800)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Colaborador readmitido com sucesso."))
                .andExpect(jsonPath("$.data.employmentCount").value(2))
                .andReturn().getResponse().getContentAsString();

        int segundo = com.jayway.jsonpath.JsonPath.read(readmitido, "$.data.id");
        int personId = com.jayway.jsonpath.JsonPath.read(readmitido, "$.data.personId");
        int personIdPrimeiro = com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(get("/api/v1/employees/{id}", primeiro).header("Authorization", "Bearer " + token))
                        .andReturn().getResponse().getContentAsString(), "$.data.personId");

        // Vinculos distintos, mesma pessoa: e isso que define a readmissao.
        org.assertj.core.api.Assertions.assertThat(segundo).isNotEqualTo(primeiro);
        org.assertj.core.api.Assertions.assertThat(personId).isEqualTo(personIdPrimeiro);

        // --- o historico da pessoa mostra os dois periodos ---
        mockMvc.perform(get("/api/v1/persons/by-cpf/{cpf}", CPF).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employments.length()").value(2))
                // Do mais recente para o mais antigo.
                .andExpect(jsonPath("$.data.employments[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.employments[1].status").value("TERMINATED"));

        // --- o vinculo antigo mantem sua timeline intacta ---
        mockMvc.perform(get("/api/v1/employees/{id}/timeline", primeiro).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].eventType").value("TERMINATION"))
                .andExpect(jsonPath("$.data[1].eventType").value("HIRE"));
    }

    @Test
    @DisplayName("RF08: desligamento anterior a admissao devolve 422")
    void naoDesligaAntesDaAdmissao() throws Exception {
        int id = admitir(CPF, "2020-02-10", "MAT-0001", 5000);

        mockMvc.perform(patch("/api/v1/employees/{id}/terminate", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"terminationDate":"2019-01-01","reason":"RESIGNATION"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://sgc.local/problems/termination-before-hire"));
    }

    @Test
    @DisplayName("RF08: motivo do desligamento e obrigatorio")
    void desligamentoExigeMotivo() throws Exception {
        int id = admitir(CPF, "2020-02-10", "MAT-0001", 5000);

        mockMvc.perform(patch("/api/v1/employees/{id}/terminate", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"terminationDate":"2023-11-30"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("reason"));
    }

    @Test
    @DisplayName("RN05: vinculo desligado nao aceita edicao")
    void desligadoNaoAceitaEdicao() throws Exception {
        int id = admitir(CPF, "2020-02-10", "MAT-0001", 5000);

        mockMvc.perform(patch("/api/v1/employees/{id}/terminate", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"terminationDate":"2023-11-30","reason":"CONTRACT_END"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/employees/{id}", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoAdmissao(CPF, "2020-02-10", "MAT-0001", 99999)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://sgc.local/problems/employment-terminated"));
    }

    @Test
    @DisplayName("RF10: o PUT nao altera mais salario, cargo nem area")
    void putNaoAlteraCamposDeCarreira() throws Exception {
        int id = admitir(CPF, "2020-02-10", "MAT-0001", 5000);

        // O corpo carrega salary, mas o DTO de edicao nao possui esse campo:
        // alterar salario passa obrigatoriamente pelo endpoint do RF10.
        mockMvc.perform(put("/api/v1/employees/{id}", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cpf":"%s","fullName":"Maria Aparecida Souza","birthDate":"1992-03-14",
                                 "phone":"31955554444","hireDate":"2020-02-10",
                                 "registrationNumber":"MAT-0001","salary":99999}
                                """.formatted(CPF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.salary").value(5000.00))
                // o que o PUT de fato edita
                .andExpect(jsonPath("$.data.phone").value("31955554444"));

        // Nenhum SALARY_CHANGE foi gerado -- so a edicao cadastral.
        mockMvc.perform(get("/api/v1/employees/{id}/timeline", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].eventType").value("HIRE"));

        mockMvc.perform(get("/api/v1/employees/{id}/timeline", id)
                        .param("includeProfileUpdates", "true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].eventType").value("PROFILE_UPDATE"));
    }

    @Test
    @DisplayName("Editar sem reenviar o endereco nao apaga o endereco cadastrado")
    void edicaoNaoApagaEndereco() throws Exception {
        String comEndereco = """
                {"cpf":"%s","fullName":"Maria Aparecida Souza","birthDate":"1992-03-14",
                 "hireDate":"2020-02-10","registrationNumber":"MAT-0001","salary":5000,
                 "address":{"street":"Rua das Flores","number":"120","city":"Belo Horizonte",
                            "state":"MG","zipCode":"30110-010"}}
                """.formatted(CPF);

        String criado = mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(comEndereco))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.address.city").value("Belo Horizonte"))
                .andReturn().getResponse().getContentAsString();

        int id = com.jayway.jsonpath.JsonPath.read(criado, "$.data.id");

        // PUT sem o campo address: o endereco deve sobreviver.
        mockMvc.perform(put("/api/v1/employees/{id}", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoAdmissao(CPF, "2020-02-10", "MAT-0001", 6000)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.address.city").value("Belo Horizonte"));
    }

    @Test
    @DisplayName("CPF com digito verificador invalido devolve 400 apontando o campo")
    void deveRecusarCpfInvalido() throws Exception {
        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cpf":"11111111111","fullName":"Teste","birthDate":"1990-01-01","hireDate":"2024-01-15"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("cpf"));
    }

    @Test
    @DisplayName("RN04: gestor de pessoas nao acessa o cadastro de usuarios")
    void gestorNaoAcessaUsuarios() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"gestor.teste@sgc.com","password":"Gestor@123",
                                 "fullName":"Gestora","role":"HR_MANAGER"}
                                """))
                .andExpect(status().isCreated());

        String gestorLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"gestor.teste@sgc.com","password":"Gestor@123"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String gestorToken = com.jayway.jsonpath.JsonPath.read(gestorLogin, "$.data.token");

        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + gestorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acesso negado"));
    }

    // ------------------------------------------------------------------

    private int admitir(String cpf, String hireDate, String matricula, int salario) throws Exception {
        String response = mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoAdmissao(cpf, hireDate, matricula, salario)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return com.jayway.jsonpath.JsonPath.read(response, "$.data.id");
    }

    private static String corpoAdmissao(String cpf, String hireDate, String matricula, int salario) {
        return """
                {"cpf":"%s","fullName":"Maria Aparecida Souza","birthDate":"1992-03-14",
                 "hireDate":"%s","registrationNumber":"%s","salary":%d}
                """.formatted(cpf, hireDate, matricula, salario);
    }
}
