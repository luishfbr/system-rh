package com.sgc.backend.service;

import com.sgc.backend.domain.entity.Department;
import com.sgc.backend.domain.entity.Employment;
import com.sgc.backend.domain.entity.EmploymentChange;
import com.sgc.backend.domain.entity.JobPosition;
import com.sgc.backend.domain.enums.ChangeStatus;
import com.sgc.backend.domain.enums.EmploymentEventType;
import com.sgc.backend.dto.request.CancelChangeRequest;
import com.sgc.backend.dto.request.CareerChangeRequest;
import com.sgc.backend.dto.response.EmploymentChangeResponse;
import com.sgc.backend.dto.response.PageResponse;
import com.sgc.backend.exception.BusinessRuleException;
import com.sgc.backend.exception.ResourceNotFoundException;
import com.sgc.backend.mapper.EmploymentMapper;
import com.sgc.backend.repository.DepartmentRepository;
import com.sgc.backend.repository.EmploymentChangeRepository;
import com.sgc.backend.repository.EmploymentRepository;
import com.sgc.backend.repository.JobPositionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Alteracoes de cargo, salario e setor (RF10).
 *
 * <p>O ponto central: uma alteracao nasce como <b>intencao</b> com data de
 * vigencia. Se a data ja chegou, e aplicada na hora; se e futura, fica PENDING
 * ate o job diario alcanca-la. Em ambos os casos o caminho e o mesmo -- a unica
 * diferenca e <i>quando</i> {@link #apply(EmploymentChange)} roda.
 *
 * <p>Regras aplicadas aqui:
 * <ul>
 *   <li><b>RN05</b> -- vinculo desligado nao recebe alteracao de carreira;</li>
 *   <li><b>RN06</b> -- toda alteracao aplicada gera evento na timeline.</li>
 * </ul>
 */
@Service
public class EmploymentChangeService {

    private static final Logger log = LoggerFactory.getLogger(EmploymentChangeService.class);

    private final EmploymentChangeRepository changeRepository;
    private final EmploymentRepository employmentRepository;
    private final JobPositionRepository jobPositionRepository;
    private final DepartmentRepository departmentRepository;
    private final EmploymentEventRecorder recorder;

    public EmploymentChangeService(EmploymentChangeRepository changeRepository,
                                   EmploymentRepository employmentRepository,
                                   JobPositionRepository jobPositionRepository,
                                   DepartmentRepository departmentRepository,
                                   EmploymentEventRecorder recorder) {
        this.changeRepository = changeRepository;
        this.employmentRepository = employmentRepository;
        this.jobPositionRepository = jobPositionRepository;
        this.departmentRepository = departmentRepository;
        this.recorder = recorder;
    }

    // ==================================================================
    // Lancamento
    // ==================================================================

    /**
     * Lanca uma alteracao de carreira.
     *
     * <p>Vigencia no passado ou hoje: aplica imediatamente. Vigencia futura:
     * fica pendente, aparecendo no relatorio "Alteracoes em andamento" (RF07)
     * ate o job diario aplica-la.
     */
    @Transactional
    public EmploymentChangeResponse schedule(Long employmentId, CareerChangeRequest request) {
        Employment employment = employmentRepository.findById(employmentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Colaborador", employmentId));

        // RN05: desligado nao recebe alteracao de cargo/salario, apenas readmissao.
        if (!employment.acceptsCareerChanges()) {
            throw new BusinessRuleException("employment-terminated",
                    "Vinculo desligado nao aceita alteracoes de carreira. Admita a pessoa novamente.");
        }

        if (!request.hasAnyTarget()) {
            throw new BusinessRuleException("change-without-target",
                    "Informe ao menos um entre salario, cargo e area de atuacao.");
        }

        if (request.effectiveDate().isBefore(employment.getHireDate())) {
            throw new BusinessRuleException("effective-before-hire",
                    "A vigencia (%s) nao pode ser anterior a admissao (%s)."
                            .formatted(request.effectiveDate(), employment.getHireDate()));
        }

        EmploymentChange change = new EmploymentChange();
        change.setEmployment(employment);
        change.setEffectiveDate(request.effectiveDate());
        change.setReason(request.reason());
        change.setNotes(request.notes());
        change.setStatus(ChangeStatus.PENDING);
        change.setNewSalary(scaled(request.salary()));
        change.setNewJobPosition(resolveJobPosition(request.jobPositionId()));
        change.setNewDepartment(resolveDepartment(request.departmentId()));

        EmploymentChange saved = changeRepository.save(change);

        // Vigencia ja alcancada: nao ha razao para esperar o job da madrugada.
        if (saved.isDue(LocalDate.now())) {
            apply(saved);
        }

        return EmploymentMapper.toChangeResponse(saved);
    }

    // ==================================================================
    // Aplicacao
    // ==================================================================

    /**
     * Faz a alteracao passar a valer.
     *
     * <p>Tres coisas acontecem juntas, e por isso na mesma transacao: o estado
     * anterior e congelado na propria alteracao, o vinculo recebe os novos
     * valores, e a timeline ganha um evento por campo alterado (RN06).
     *
     * <p>Um campo cujo valor solicitado ja e igual ao atual nao gera evento --
     * registrar "salario mudou de 9200 para 9200" so sujaria o historico.
     */
    @Transactional
    public void apply(EmploymentChange change) {
        Employment employment = change.getEmployment();

        // Congela o "antes" agora, no momento em que a mudanca de fato ocorre.
        change.captureCurrentState(employment);

        if (change.changesSalary() && !sameAmount(employment.getSalary(), change.getNewSalary())) {
            BigDecimal previous = employment.getSalary();
            employment.setSalary(change.getNewSalary());
            recorder.recordAppliedChange(change, EmploymentEventType.SALARY_CHANGE,
                    "salary", previous, change.getNewSalary());
        }

        if (change.changesJobPosition() && !sameId(employment.getJobPosition(), change.getNewJobPosition())) {
            JobPosition previous = employment.getJobPosition();
            employment.setJobPosition(change.getNewJobPosition());
            recorder.recordAppliedChange(change, EmploymentEventType.POSITION_CHANGE,
                    "jobPosition",
                    previous == null ? null : previous.getTitle(),
                    change.getNewJobPosition().getTitle());
        }

        if (change.changesDepartment() && !sameId(employment.getDepartment(), change.getNewDepartment())) {
            Department previous = employment.getDepartment();
            employment.setDepartment(change.getNewDepartment());
            recorder.recordAppliedChange(change, EmploymentEventType.DEPARTMENT_CHANGE,
                    "department",
                    previous == null ? null : previous.getName(),
                    change.getNewDepartment().getName());
        }

        change.markApplied();
        log.info("Alteracao {} aplicada ao vinculo {} (vigencia {})",
                change.getId(), employment.getId(), change.getEffectiveDate());
    }

    /**
     * Aplica todas as alteracoes vencidas -- chamado pelo job diario.
     *
     * <p>A ordem cronologica importa: se um vinculo tem duas alteracoes vencidas
     * (porque o job ficou dias sem rodar), aplicar fora de ordem registraria um
     * "valor anterior" que nunca existiu.
     *
     * @return quantas alteracoes foram aplicadas
     */
    @Transactional
    public int applyDue(LocalDate reference) {
        List<EmploymentChange> due = changeRepository.findDue(ChangeStatus.PENDING, reference);

        for (EmploymentChange change : due) {
            // Um vinculo desligado entre o lancamento e a vigencia: a alteracao
            // perde o sentido e e cancelada, em vez de aplicada a quem ja saiu.
            if (!change.getEmployment().acceptsCareerChanges()) {
                change.cancel("Cancelada automaticamente: vinculo encerrado antes da vigencia.");
                log.info("Alteracao {} cancelada -- vinculo {} nao esta mais ativo",
                        change.getId(), change.getEmployment().getId());
                continue;
            }
            apply(change);
        }

        return due.size();
    }

    // ==================================================================
    // Cancelamento
    // ==================================================================

    /**
     * Cancela uma alteracao ainda pendente.
     *
     * <p>Aplicada nao se cancela: ja virou evento imutavel na timeline. Para
     * desfazer o efeito, lance outra alteracao no sentido contrario -- e assim
     * que um historico honesto se comporta.
     */
    @Transactional
    public EmploymentChangeResponse cancel(Long changeId, CancelChangeRequest request) {
        EmploymentChange change = findEntity(changeId);

        if (change.getStatus() == ChangeStatus.CANCELLED) {
            throw new BusinessRuleException("already-cancelled", "Esta alteracao ja foi cancelada.");
        }
        if (change.getStatus() == ChangeStatus.APPLIED) {
            throw new BusinessRuleException("change-already-applied",
                    "Esta alteracao ja entrou em vigor em %s e nao pode ser cancelada. "
                            .formatted(change.getEffectiveDate())
                            + "Lance uma nova alteracao para reverter o efeito.");
        }

        change.cancel(request.reason());
        return EmploymentMapper.toChangeResponse(change);
    }

    // ==================================================================
    // Consultas
    // ==================================================================

    /** Historico de alteracoes de um vinculo, incluindo pendentes e canceladas. */
    @Transactional(readOnly = true)
    public List<EmploymentChangeResponse> listByEmployment(Long employmentId) {
        if (!employmentRepository.existsById(employmentId)) {
            throw ResourceNotFoundException.of("Colaborador", employmentId);
        }
        return changeRepository.findByEmploymentIdOrderByEffectiveDateDescIdDesc(employmentId).stream()
                .map(EmploymentMapper::toChangeResponse)
                .toList();
    }

    /**
     * Fila de alteracoes por situacao.
     *
     * <p>Com {@code PENDING}, e o relatorio "Alteracoes em andamento" do RF07:
     * tudo que foi decidido e ainda nao entrou em vigor.
     */
    @Transactional(readOnly = true)
    public PageResponse<EmploymentChangeResponse> listByStatus(ChangeStatus status, Pageable pageable) {
        return PageResponse.from(
                changeRepository.findByStatusWithDetails(status, pageable),
                EmploymentMapper::toChangeResponse);
    }

    @Transactional(readOnly = true)
    public EmploymentChangeResponse findById(Long changeId) {
        return EmploymentMapper.toChangeResponse(findEntity(changeId));
    }

    // ==================================================================

    private EmploymentChange findEntity(Long id) {
        return changeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Alteracao", id));
    }

    private JobPosition resolveJobPosition(Long id) {
        return id == null ? null : jobPositionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Cargo", id));
    }

    private Department resolveDepartment(Long id) {
        return id == null ? null : departmentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Area de atuacao", id));
    }

    /** Escala 2, para bater com a coluna NUMERIC(12,2) e com o vinculo. */
    private static BigDecimal scaled(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    /** Dinheiro compara-se por valor: 9200 e 9200.00 sao o mesmo salario. */
    private static boolean sameAmount(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.compareTo(b) == 0;
    }

    private static boolean sameId(com.sgc.backend.domain.entity.BaseEntity a,
                                  com.sgc.backend.domain.entity.BaseEntity b) {
        return Objects.equals(a == null ? null : a.getId(), b == null ? null : b.getId());
    }
}
