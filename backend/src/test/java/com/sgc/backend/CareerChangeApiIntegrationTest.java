package com.sgc.backend;

import com.sgc.backend.domain.enums.ChangeStatus;
import com.sgc.backend.repository.EmploymentRepository;
import com.sgc.backend.repository.PersonRepository;
import com.sgc.backend.service.EmploymentChangeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Alteracoes de carreira ponta a ponta (RF10).
 *
 * <p>O job agendado esta desligado no perfil de teste
 * ({@code sgc.scheduling.enabled=false}); os casos chamam
 * {@link EmploymentChangeService#applyDue(LocalDate)} diretamente, passando a
 * data desejada. Assim o comportamento "quando a vigencia chega" e testado sem
 * depender do relogio nem esperar por cron.
 */
@AutoConfigureMockMvc
class CareerChangeApiIntegrationTest extends AbstractIntegrationTest {

    private static final String CPF = "111.444.777-35";

    @Autowired private MockMvc mockMvc;
    @Autowired private EmploymentRepository employmentRepository;
    @Autowired private PersonRepository personRepository;
    @Autowired private EmploymentChangeService changeService;

    private String token;
    private int employmentId;

    @BeforeEach
    void prepararColaborador() throws Exception {
        employmentRepository.deleteAll();
        personRepository.deleteAll();

        String login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@sgc.com","password":"Admin@123"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        token = com.jayway.jsonpath.JsonPath.read(login, "$.data.token");

        String admitido = mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cpf":"%s","fullName":"Maria Aparecida Souza","birthDate":"1992-03-14",
                                 "hireDate":"2024-01-15","registrationNumber":"MAT-0001",
                                 "salary":7500.00,"departmentId":1,"jobPositionId":1,"unitId":1}
                                """.formatted(CPF)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        employmentId = com.jayway.jsonpath.JsonPath.read(admitido, "$.data.id");
    }

    @Test
    @DisplayName("Vigencia futura fica PENDING e nao altera o colaborador ainda")
    void alteracaoFuturaFicaPendente() throws Exception {
        LocalDate futuro = LocalDate.now().plusDays(30);

        mockMvc.perform(post("/api/v1/employees/{id}/career-changes", employmentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"effectiveDate":"%s","reason":"PROMOTION","salary":9200.00,
                                 "jobPositionId":6,"notes":"Ciclo 2026/1"}
                                """.formatted(futuro)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.daysUntilEffective").value(30))
                // O estado anterior so e capturado na aplicacao.
                .andExpect(jsonPath("$.data.previousSalary").doesNotExist());

        // O vinculo continua como estava.
        mockMvc.perform(get("/api/v1/employees/{id}", employmentId).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.salary").value(7500.00))
                .andExpect(jsonPath("$.data.jobPosition.name").value("Analista de RH"));

        // E a timeline ainda nao registrou nada: nao aconteceu.
        mockMvc.perform(get("/api/v1/employees/{id}/timeline", employmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].eventType").value("HIRE"));
    }

    @Test
    @DisplayName("RF07: a fila de pendentes alimenta 'Alteracoes em andamento'")
    void filaDeAlteracoesEmAndamento() throws Exception {
        agendar(LocalDate.now().plusDays(30), "PROMOTION", "\"salary\":9200.00");

        mockMvc.perform(get("/api/v1/career-changes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].employeeName").value("Maria Aparecida Souza"))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("Vigencia retroativa aplica na hora e usa a data de vigencia no evento")
    void alteracaoRetroativaAplicaImediatamente() throws Exception {
        mockMvc.perform(post("/api/v1/employees/{id}/career-changes", employmentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"effectiveDate":"2026-06-01","reason":"COLLECTIVE_AGREEMENT",
                                 "salary":7950.00,"notes":"Dissidio"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("APPLIED"))
                .andExpect(jsonPath("$.data.previousSalary").value(7500.00))
                .andExpect(jsonPath("$.data.appliedAt").exists());

        mockMvc.perform(get("/api/v1/employees/{id}", employmentId).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.salary").value(7950.00));

        // O evento carrega a data em que passou a valer, nao a do lancamento.
        mockMvc.perform(get("/api/v1/employees/{id}/timeline", employmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data[0].eventType").value("SALARY_CHANGE"))
                .andExpect(jsonPath("$.data[0].effectiveDate").value("2026-06-01"))
                .andExpect(jsonPath("$.data[0].reason").value("COLLECTIVE_AGREEMENT"))
                .andExpect(jsonPath("$.data[0].changeId").exists());
    }

    @Test
    @DisplayName("Quando a vigencia chega, o job aplica e a fila esvazia")
    void jobAplicaAlteracaoVencida() throws Exception {
        LocalDate futuro = LocalDate.now().plusDays(10);
        agendar(futuro, "PROMOTION", "\"salary\":9200.00,\"jobPositionId\":6");

        // Simula o dia da vigencia chegando.
        int aplicadas = changeService.applyDue(futuro);
        org.assertj.core.api.Assertions.assertThat(aplicadas).isEqualTo(1);

        mockMvc.perform(get("/api/v1/employees/{id}", employmentId).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.salary").value(9200.00))
                .andExpect(jsonPath("$.data.jobPosition.name").value("Gerente"));

        // Uma alteracao que mexe em dois campos gera um evento para cada.
        mockMvc.perform(get("/api/v1/employees/{id}/timeline", employmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.length()").value(3));

        mockMvc.perform(get("/api/v1/career-changes").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @DisplayName("Alteracoes vencidas sao aplicadas em ordem cronologica")
    void aplicaEmOrdemCronologica() throws Exception {
        // Lancadas fora de ordem de proposito.
        agendar(LocalDate.now().plusDays(20), "PROMOTION", "\"salary\":9200.00");
        agendar(LocalDate.now().plusDays(10), "MERIT_INCREASE", "\"salary\":8000.00");

        changeService.applyDue(LocalDate.now().plusDays(30));

        // O encadeamento so fica correto se a de 10 dias rodar antes: 7500 -> 8000 -> 9200.
        mockMvc.perform(get("/api/v1/employees/{id}/career-changes", employmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                // Ordenado da mais recente para a mais antiga.
                .andExpect(jsonPath("$.data[0].previousSalary").value(8000.00))
                .andExpect(jsonPath("$.data[0].newSalary").value(9200.00))
                .andExpect(jsonPath("$.data[1].previousSalary").value(7500.00))
                .andExpect(jsonPath("$.data[1].newSalary").value(8000.00));
    }

    @Test
    @DisplayName("Alteracao pendente pode ser cancelada; aplicada nao")
    void cancelamento() throws Exception {
        int changeId = agendar(LocalDate.now().plusDays(30), "TRANSFER", "\"departmentId\":3");

        mockMvc.perform(patch("/api/v1/career-changes/{id}/cancel", changeId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Adiada para o proximo ciclo"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        // Cancelada nao entra na fila do job.
        org.assertj.core.api.Assertions.assertThat(
                changeService.applyDue(LocalDate.now().plusDays(60))).isZero();

        int aplicada = agendar(LocalDate.now(), "MERIT_INCREASE", "\"salary\":8100.00");
        mockMvc.perform(patch("/api/v1/career-changes/{id}/cancel", aplicada)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"tentativa"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://sgc.local/problems/change-already-applied"));
    }

    @Test
    @DisplayName("RN05: vinculo desligado nao aceita alteracao de carreira")
    void desligadoNaoAceitaAlteracao() throws Exception {
        mockMvc.perform(patch("/api/v1/employees/{id}/terminate", employmentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"terminationDate":"2026-08-31","reason":"RESIGNATION"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/employees/{id}/career-changes", employmentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"effectiveDate":"%s","reason":"PROMOTION","salary":9200.00}
                                """.formatted(LocalDate.now().plusDays(10))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://sgc.local/problems/employment-terminated"));
    }

    @Test
    @DisplayName("Desligamento antes da vigencia cancela a alteracao automaticamente")
    void desligamentoCancelaAlteracaoPendente() throws Exception {
        LocalDate futuro = LocalDate.now().plusDays(30);
        agendar(futuro, "MERIT_INCREASE", "\"salary\":11000.00");

        mockMvc.perform(patch("/api/v1/employees/{id}/terminate", employmentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"terminationDate":"2026-08-31","reason":"RESIGNATION"}
                                """))
                .andExpect(status().isOk());

        changeService.applyDue(futuro);

        // Aplicar um aumento a quem ja saiu nao faria sentido.
        mockMvc.perform(get("/api/v1/employees/{id}/career-changes", employmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data[0].status").value("CANCELLED"))
                .andExpect(jsonPath("$.data[0].cancellationReason")
                        .value(org.hamcrest.Matchers.containsString("vinculo encerrado")));
    }

    @Test
    @DisplayName("Alteracao sem nenhum campo de destino e recusada")
    void alteracaoSemDestino() throws Exception {
        mockMvc.perform(post("/api/v1/employees/{id}/career-changes", employmentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"effectiveDate":"%s","reason":"OTHER"}
                                """.formatted(LocalDate.now().plusDays(10))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://sgc.local/problems/change-without-target"));
    }

    @Test
    @DisplayName("Vigencia anterior a admissao e recusada")
    void vigenciaAnteriorAAdmissao() throws Exception {
        mockMvc.perform(post("/api/v1/employees/{id}/career-changes", employmentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"effectiveDate":"2020-01-01","reason":"PROMOTION","salary":8000.00}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://sgc.local/problems/effective-before-hire"));
    }

    @Test
    @DisplayName("Valor solicitado igual ao atual nao gera evento")
    void valorIgualNaoGeraEvento() throws Exception {
        agendar(LocalDate.now(), "MERIT_INCREASE", "\"salary\":7500.00");

        // Registrar "mudou de 7500 para 7500" so sujaria o historico.
        mockMvc.perform(get("/api/v1/employees/{id}/timeline", employmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].eventType").value("HIRE"));
    }

    // ------------------------------------------------------------------

    private int agendar(LocalDate effectiveDate, String reason, String targetJson) throws Exception {
        String response = mockMvc.perform(post("/api/v1/employees/{id}/career-changes", employmentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"effectiveDate":"%s","reason":"%s",%s}
                                """.formatted(effectiveDate, reason, targetJson)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return com.jayway.jsonpath.JsonPath.read(response, "$.data.id");
    }
}
