package com.sgc.backend.service;

import com.sgc.backend.domain.entity.Employment;
import com.sgc.backend.domain.entity.Person;
import com.sgc.backend.domain.enums.EmploymentStatus;
import com.sgc.backend.domain.enums.TerminationReason;
import com.sgc.backend.dto.request.CreateEmployeeRequest;
import com.sgc.backend.dto.request.TerminateEmploymentRequest;
import com.sgc.backend.dto.request.UpdateEmployeeRequest;
import com.sgc.backend.exception.BusinessRuleException;
import com.sgc.backend.exception.ResourceNotFoundException;
import com.sgc.backend.repository.DepartmentRepository;
import com.sgc.backend.repository.EmploymentEventRepository;
import com.sgc.backend.repository.EmploymentRepository;
import com.sgc.backend.repository.JobPositionRepository;
import com.sgc.backend.repository.PersonRepository;
import com.sgc.backend.repository.UnitRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regras de negocio da admissao e do desligamento (RF08), com os repositories
 * substituidos por mocks.
 *
 * <p>Sem banco e sem contexto do Spring: o objetivo e provar que as regras
 * decidem certo, nao que o JPA funciona. Quando um caso falha, a causa esta na
 * regra e em nenhum outro lugar.
 */
@ExtendWith(MockitoExtension.class)
class EmploymentServiceTest {

    private static final String CPF = "11144477735";

    @Mock private EmploymentRepository employmentRepository;
    @Mock private PersonRepository personRepository;
    @Mock private EmploymentEventRepository eventRepository;
    @Mock private EmploymentEventRecorder recorder;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private JobPositionRepository jobPositionRepository;
    @Mock private UnitRepository unitRepository;

    @InjectMocks private EmploymentService employmentService;

    @Nested
    @DisplayName("RF08 - admissao")
    class Admit {

        @Test
        @DisplayName("CPF novo cria a pessoa e o primeiro vinculo")
        void deveCriarPessoaEVinculo() {
            when(personRepository.findByCpf(CPF)).thenReturn(Optional.empty());
            when(personRepository.save(any(Person.class))).thenAnswer(i -> i.getArgument(0));
            when(employmentRepository.save(any(Employment.class))).thenAnswer(i -> i.getArgument(0));
            when(employmentRepository.findByPersonIdOrderByHireDateDescIdDesc(any())).thenReturn(List.of());

            employmentService.admit(createRequest(CPF));

            verify(personRepository).save(ArgumentMatchers.argThat(p -> CPF.equals(p.getCpf())));
            // RN06: a timeline comeca pela admissao.
            verify(recorder).recordHire(any(Employment.class));
        }

        @Test
        @DisplayName("RN07: CPF com vinculo em aberto nao pode ser admitido de novo")
        void deveRecusarCpfComVinculoAberto() {
            Person existing = person(1L);
            when(personRepository.findByCpf(CPF)).thenReturn(Optional.of(existing));
            when(employmentRepository.existsByPersonIdAndStatusNot(1L, EmploymentStatus.TERMINATED)).thenReturn(true);

            assertThatThrownBy(() -> employmentService.admit(createRequest(CPF)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("ja possui um vinculo ativo");

            verify(employmentRepository, never()).save(any());
            verify(recorder, never()).recordHire(any());
        }

        @Test
        @DisplayName("Readmissao reaproveita a pessoa e cria um vinculo novo")
        void deveReadmitirReaproveitandoAPessoa() {
            Person existing = person(1L);
            when(personRepository.findByCpf(CPF)).thenReturn(Optional.of(existing));
            when(employmentRepository.existsByPersonIdAndStatusNot(1L, EmploymentStatus.TERMINATED)).thenReturn(false);
            when(employmentRepository.save(any(Employment.class))).thenAnswer(i -> i.getArgument(0));
            when(employmentRepository.findByPersonIdOrderByHireDateDescIdDesc(1L)).thenReturn(List.of());

            employmentService.admit(createRequest(CPF));

            // A pessoa ja existia: nao se cria outra, o vinculo e que e novo.
            verify(personRepository, never()).save(any(Person.class));
            verify(employmentRepository).save(ArgumentMatchers.argThat(e -> e.getPerson() == existing));
            verify(recorder).recordHire(any(Employment.class));
        }

        @Test
        @DisplayName("Normaliza o CPF, para nao driblar a unicidade da pessoa")
        void deveNormalizarCpf() {
            when(personRepository.findByCpf(CPF)).thenReturn(Optional.empty());
            when(personRepository.save(any(Person.class))).thenAnswer(i -> i.getArgument(0));
            when(employmentRepository.save(any(Employment.class))).thenAnswer(i -> i.getArgument(0));
            when(employmentRepository.findByPersonIdOrderByHireDateDescIdDesc(any())).thenReturn(List.of());

            employmentService.admit(createRequest("111.444.777-35"));

            verify(personRepository).save(ArgumentMatchers.argThat(p -> CPF.equals(p.getCpf())));
        }
    }

    @Nested
    @DisplayName("RF08 - desligamento")
    class Terminate {

        @Test
        void deveDesligarRegistrandoDataMotivoEEvento() {
            Employment employment = employment(EmploymentStatus.ACTIVE, LocalDate.of(2020, 2, 10));
            when(employmentRepository.findById(1L)).thenReturn(Optional.of(employment));
            when(employmentRepository.findByPersonIdOrderByHireDateDescIdDesc(any())).thenReturn(List.of());

            employmentService.terminate(1L, new TerminateEmploymentRequest(
                    LocalDate.of(2023, 11, 30), TerminationReason.RESIGNATION, "proposta externa"));

            assertThat(employment.getStatus()).isEqualTo(EmploymentStatus.TERMINATED);
            assertThat(employment.getTerminationDate()).isEqualTo(LocalDate.of(2023, 11, 30));
            assertThat(employment.getTerminationReason()).isEqualTo(TerminationReason.RESIGNATION);
            verify(recorder).recordTermination(employment, LocalDate.of(2023, 11, 30),
                    TerminationReason.RESIGNATION, "proposta externa");
        }

        @Test
        @DisplayName("Desligamento anterior a admissao e recusado")
        void deveRecusarDataAnteriorAAdmissao() {
            Employment employment = employment(EmploymentStatus.ACTIVE, LocalDate.of(2020, 2, 10));
            when(employmentRepository.findById(1L)).thenReturn(Optional.of(employment));

            assertThatThrownBy(() -> employmentService.terminate(1L, new TerminateEmploymentRequest(
                    LocalDate.of(2019, 1, 1), TerminationReason.RESIGNATION, null)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("nao pode ser anterior a admissao");

            verify(recorder, never()).recordTermination(any(), any(), any(), any());
        }

        @Test
        void deveRecusarDesligarDuasVezes() {
            Employment employment = employment(EmploymentStatus.TERMINATED, LocalDate.of(2020, 2, 10));
            employment.setTerminationDate(LocalDate.of(2023, 11, 30));
            when(employmentRepository.findById(1L)).thenReturn(Optional.of(employment));

            assertThatThrownBy(() -> employmentService.terminate(1L, new TerminateEmploymentRequest(
                    LocalDate.of(2024, 1, 1), TerminationReason.OTHER, null)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("ja esta desligado");
        }

        @Test
        void deveFalharQuandoVinculoNaoExiste() {
            when(employmentRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> employmentService.terminate(99L, new TerminateEmploymentRequest(
                    LocalDate.now(), TerminationReason.OTHER, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("RF05 / RF06")
    class UpdateAndInactivate {

        @Test
        @DisplayName("RN05: vinculo desligado nao aceita edicao")
        void deveRecusarEdicaoDeDesligado() {
            Employment employment = employment(EmploymentStatus.TERMINATED, LocalDate.of(2020, 2, 10));
            when(employmentRepository.findById(1L)).thenReturn(Optional.of(employment));

            assertThatThrownBy(() -> employmentService.update(1L, updateRequest()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("desligado");
        }

        @Test
        void deveInativarERegistrarEvento() {
            Employment employment = employment(EmploymentStatus.ACTIVE, LocalDate.of(2020, 2, 10));
            when(employmentRepository.findById(1L)).thenReturn(Optional.of(employment));
            when(employmentRepository.findByPersonIdOrderByHireDateDescIdDesc(any())).thenReturn(List.of());

            employmentService.inactivate(1L);

            assertThat(employment.getStatus()).isEqualTo(EmploymentStatus.INACTIVE);
            verify(recorder).recordInactivation(employment);
        }

        @Test
        @DisplayName("Desligado nao e a mesma coisa que inativo")
        void deveRecusarInativarDesligado() {
            Employment employment = employment(EmploymentStatus.TERMINATED, LocalDate.of(2020, 2, 10));
            when(employmentRepository.findById(1L)).thenReturn(Optional.of(employment));

            assertThatThrownBy(() -> employmentService.inactivate(1L))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void deveRecusarReativarQuemNaoEstaInativo() {
            Employment employment = employment(EmploymentStatus.ACTIVE, LocalDate.of(2020, 2, 10));
            when(employmentRepository.findById(1L)).thenReturn(Optional.of(employment));

            assertThatThrownBy(() -> employmentService.activate(1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("inativos");
        }
    }

    // ------------------------------------------------------------------

    private static Person person(Long id) {
        Person person = new Person();
        person.setId(id);
        person.setCpf(CPF);
        person.setFullName("Maria Souza");
        person.setBirthDate(LocalDate.of(1992, 3, 14));
        return person;
    }

    private static Employment employment(EmploymentStatus status, LocalDate hireDate) {
        Employment employment = new Employment();
        employment.setId(1L);
        employment.setPerson(person(1L));
        employment.setStatus(status);
        employment.setHireDate(hireDate);
        return employment;
    }

    private static CreateEmployeeRequest createRequest(String cpf) {
        return new CreateEmployeeRequest(
                cpf, null, "Maria Souza", LocalDate.of(1992, 3, 14), null, null, null,
                null, null, null,
                null, null, LocalDate.of(2020, 2, 10), null, null, null, null, null,
                null, null, null, null);
    }

    /**
     * O RF10 retirou salario, cargo e area deste DTO -- por isso ele tem menos
     * campos do que o de admissao.
     */
    private static UpdateEmployeeRequest updateRequest() {
        return new UpdateEmployeeRequest(
                CPF, null, "Maria Souza", LocalDate.of(1992, 3, 14), null, null, null,
                null, null, null,
                null, null, LocalDate.of(2020, 2, 10), null, null, null, null,
                null, null);
    }
}
