package com.sgc.backend.service;

import com.sgc.backend.domain.entity.Department;
import com.sgc.backend.domain.entity.Employment;
import com.sgc.backend.domain.entity.EmploymentEvent;
import com.sgc.backend.domain.entity.JobPosition;
import com.sgc.backend.domain.entity.Person;
import com.sgc.backend.domain.entity.Unit;
import com.sgc.backend.domain.enums.EmploymentEventType;
import com.sgc.backend.domain.enums.EmploymentStatus;
import com.sgc.backend.domain.enums.Gender;
import com.sgc.backend.dto.request.AddressRequest;
import com.sgc.backend.dto.request.CreateEmployeeRequest;
import com.sgc.backend.dto.request.TerminateEmploymentRequest;
import com.sgc.backend.dto.request.UpdateEmployeeRequest;
import com.sgc.backend.dto.response.EmployeeResponse;
import com.sgc.backend.dto.response.EmployeeSummaryResponse;
import com.sgc.backend.dto.response.EmploymentEventResponse;
import com.sgc.backend.dto.response.PageResponse;
import com.sgc.backend.exception.BusinessRuleException;
import com.sgc.backend.exception.DuplicateResourceException;
import com.sgc.backend.exception.ResourceNotFoundException;
import com.sgc.backend.mapper.EmploymentMapper;
import com.sgc.backend.mapper.PersonMapper;
import com.sgc.backend.repository.DepartmentRepository;
import com.sgc.backend.repository.EmploymentEventRepository;
import com.sgc.backend.repository.EmploymentRepository;
import com.sgc.backend.repository.EmploymentSpecifications;
import com.sgc.backend.repository.JobPositionRepository;
import com.sgc.backend.repository.PersonRepository;
import com.sgc.backend.repository.UnitRepository;
import com.sgc.backend.validation.CpfValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Admissao, edicao, desligamento e inativacao de colaboradores
 * (RF04, RF05, RF06, RF08).
 *
 * <p>"Colaborador", na API, e o <b>vinculo</b>: um periodo de contrato entre a
 * empresa e uma pessoa. Isso torna a readmissao uma operacao comum -- basta
 * admitir de novo o mesmo CPF, e um vinculo novo nasce ao lado do antigo, que
 * fica preservado.
 *
 * <p>Regras aplicadas aqui:
 * <ul>
 *   <li><b>RN02</b> -- o CPF identifica a pessoa, que existe uma unica vez;</li>
 *   <li><b>RN05</b> -- vinculo desligado nao aceita alteracoes;</li>
 *   <li><b>RN06</b> -- toda mudanca de situacao, cargo, salario ou setor gera
 *       registro na timeline;</li>
 *   <li><b>RN07</b> -- nao se admite quem ja tem vinculo em aberto. O indice
 *       parcial {@code uk_employments_one_open_per_person} garante isso no banco;
 *       a checagem aqui existe para produzir a mensagem legivel.</li>
 * </ul>
 */
@Service
public class EmploymentService {

    private final EmploymentRepository employmentRepository;
    private final PersonRepository personRepository;
    private final EmploymentEventRepository eventRepository;
    private final EmploymentEventRecorder recorder;
    private final DepartmentRepository departmentRepository;
    private final JobPositionRepository jobPositionRepository;
    private final UnitRepository unitRepository;

    public EmploymentService(EmploymentRepository employmentRepository,
                             PersonRepository personRepository,
                             EmploymentEventRepository eventRepository,
                             EmploymentEventRecorder recorder,
                             DepartmentRepository departmentRepository,
                             JobPositionRepository jobPositionRepository,
                             UnitRepository unitRepository) {
        this.employmentRepository = employmentRepository;
        this.personRepository = personRepository;
        this.eventRepository = eventRepository;
        this.recorder = recorder;
        this.departmentRepository = departmentRepository;
        this.jobPositionRepository = jobPositionRepository;
        this.unitRepository = unitRepository;
    }

    // ==================================================================
    // RF04 / RF08 -- admitir
    // ==================================================================

    /**
     * Admite um colaborador.
     *
     * <p>Serve tanto para a primeira contratacao quanto para a readmissao: se o
     * CPF ja existe, a pessoa e reaproveitada e ganha um vinculo novo. E por isso
     * que nao ha endpoint separado de "readmissao" -- readmitir e admitir de novo.
     */
    @Transactional
    public EmployeeResponse admit(CreateEmployeeRequest request) {
        String cpf = CpfValidator.normalize(request.cpf());

        Person person = personRepository.findByCpf(cpf).orElse(null);
        boolean readmission = person != null;

        if (readmission) {
            // RN07: so pode haver um vinculo em aberto por pessoa.
            if (employmentRepository.existsByPersonIdAndStatusNot(person.getId(), EmploymentStatus.TERMINATED)) {
                throw new BusinessRuleException("cpf-already-active",
                        "Este CPF ja possui um vinculo ativo. Desligue o vinculo atual antes de admitir novamente.");
            }
            // Readmissao atualiza o cadastro pessoal com os dados recoletados.
            applyPersonData(person, request.rg(), request.fullName(), request.birthDate(),
                    request.personalEmail(), request.gender(), request.phone(),
                    request.dependentsCount(), request.driverLicenseCategories());
            // Endereco nulo aqui significa "nao reinformado", nunca "apagar":
            // seria destrutivo perder o endereco de quem esta voltando.
            if (request.address() != null) {
                person.setAddress(PersonMapper.applyAddress(person.getAddress(), request.address()));
            }
        } else {
            person = new Person();
            person.setCpf(cpf);
            applyPersonData(person, request.rg(), request.fullName(), request.birthDate(),
                    request.personalEmail(), request.gender(), request.phone(),
                    request.dependentsCount(), request.driverLicenseCategories());
            if (request.address() != null) {
                person.setAddress(PersonMapper.applyAddress(null, request.address()));
            }
            person = personRepository.save(person);
        }

        checkRegistrationNumber(request.registrationNumber(), null);
        checkCorporateEmail(request.corporateEmail(), null);

        Employment employment = new Employment();
        employment.setPerson(person);
        employment.setStatus(EmploymentStatus.ACTIVE);
        applyEmploymentData(employment, request.registrationNumber(), request.corporateEmail(),
                request.hireDate(), request.salary(), request.bonus(), request.weeklyHours(),
                request.grade(), request.band(), request.departmentId(), request.jobPositionId(),
                request.unitId(), request.notes());

        Employment saved = employmentRepository.save(employment);

        // RN06: a timeline comeca pela admissao.
        recorder.recordHire(saved);

        return EmploymentMapper.toResponse(saved, countEmployments(person.getId()));
    }

    // ==================================================================
    // RF08 -- desligar
    // ==================================================================

    /**
     * Desliga o colaborador, encerrando o vinculo.
     *
     * <p>O registro nao e apagado: ele passa a TERMINATED e continua no historico
     * da pessoa. E o desligamento que libera o CPF para uma futura readmissao,
     * porque o indice parcial so conta vinculos nao-desligados.
     */
    @Transactional
    public EmployeeResponse terminate(Long employmentId, TerminateEmploymentRequest request) {
        Employment employment = findEntity(employmentId);

        if (employment.getStatus() == EmploymentStatus.TERMINATED) {
            throw new BusinessRuleException("already-terminated",
                    "Este vinculo ja esta desligado desde " + employment.getTerminationDate() + ".");
        }
        if (request.terminationDate().isBefore(employment.getHireDate())) {
            throw new BusinessRuleException("termination-before-hire",
                    "A data de desligamento (%s) nao pode ser anterior a admissao (%s)."
                            .formatted(request.terminationDate(), employment.getHireDate()));
        }

        employment.terminate(request.terminationDate(), request.reason(), request.notes());

        recorder.recordTermination(employment, request.terminationDate(), request.reason(), request.notes());

        return EmploymentMapper.toResponse(employment, countEmployments(employment.getPerson().getId()));
    }

    // ==================================================================
    // RF05 -- editar
    // ==================================================================

    @Transactional
    public EmployeeResponse update(Long employmentId, UpdateEmployeeRequest request) {
        Employment employment = findEntity(employmentId);
        Person person = employment.getPerson();

        // RN05: desligado so volta por readmissao (um vinculo novo).
        if (!employment.acceptsCareerChanges()) {
            throw new BusinessRuleException("employment-terminated",
                    "Vinculo desligado nao aceita alteracoes. Admita a pessoa novamente para criar um novo vinculo.");
        }

        String cpf = CpfValidator.normalize(request.cpf());
        if (personRepository.existsByCpfAndIdNot(cpf, person.getId())) {
            throw new DuplicateResourceException("cpf", "Ja existe outra pessoa cadastrada com este CPF.");
        }

        checkRegistrationNumber(request.registrationNumber(), employmentId);
        checkCorporateEmail(request.corporateEmail(), employmentId);

        // --- estado anterior, para o diff da timeline ---
        Map<String, Object> before = profileSnapshot(person, employment);

        // --- aplica ---
        person.setCpf(cpf);
        applyPersonData(person, request.rg(), request.fullName(), request.birthDate(),
                request.personalEmail(), request.gender(), request.phone(),
                request.dependentsCount(), request.driverLicenseCategories());
        // Endereco ausente significa "nao informado", nunca "apagar".
        //
        // A semantica estrita de PUT diria o contrario -- substituicao total --,
        // mas aqui ela custaria caro: um gestor que atualiza apenas o salario e
        // omite o endereco perderia o dado sem perceber. Perda silenciosa de
        // dado pessoal e pior do que a impureza REST. Remover endereco nao e
        // requisito de nenhum RF; se vier a ser, merece endpoint proprio.
        if (request.address() != null) {
            person.setAddress(PersonMapper.applyAddress(person.getAddress(), request.address()));
        }

        // Salario, cargo e area NAO sao tocados aqui: pertencem ao RF10, que exige
        // motivo e data de vigencia (ver EmploymentChangeService).
        applyEditableEmploymentData(employment, request.registrationNumber(), request.corporateEmail(),
                request.hireDate(), request.bonus(), request.weeklyHours(),
                request.grade(), request.band(), request.unitId(), request.notes());

        Map<String, Object> after = profileSnapshot(person, employment);
        Diff diff = Diff.between(before, after);
        recorder.recordProfileUpdate(employment, diff.previous(), diff.current());

        return EmploymentMapper.toResponse(employment, countEmployments(person.getId()));
    }

    // ==================================================================
    // RF06 -- inativar / reativar
    // ==================================================================

    @Transactional
    public EmployeeResponse inactivate(Long employmentId) {
        Employment employment = findEntity(employmentId);

        if (employment.getStatus() == EmploymentStatus.INACTIVE) {
            throw new BusinessRuleException("already-inactive", "Este colaborador ja esta inativo.");
        }
        if (employment.getStatus() == EmploymentStatus.TERMINATED) {
            throw new BusinessRuleException("employment-terminated",
                    "Vinculo desligado nao pode ser inativado.");
        }

        employment.setStatus(EmploymentStatus.INACTIVE);
        recorder.recordInactivation(employment);

        return EmploymentMapper.toResponse(employment, countEmployments(employment.getPerson().getId()));
    }

    @Transactional
    public EmployeeResponse activate(Long employmentId) {
        Employment employment = findEntity(employmentId);

        if (employment.getStatus() != EmploymentStatus.INACTIVE) {
            throw new BusinessRuleException("not-inactive",
                    "Apenas colaboradores inativos podem ser reativados por esta operacao.");
        }

        employment.setStatus(EmploymentStatus.ACTIVE);
        recorder.recordActivation(employment);

        return EmploymentMapper.toResponse(employment, countEmployments(employment.getPerson().getId()));
    }

    /**
     * Exclusao definitiva (RN04) -- restrita a administradores pelo controller.
     *
     * <p>Remove o vinculo e sua timeline (cascade no banco). A pessoa permanece,
     * porque pode ter outros periodos; se este era o unico, o registro dela fica
     * sem vinculos e pode ser purgado a parte.
     */
    @Transactional
    public void delete(Long employmentId) {
        employmentRepository.delete(findEntity(employmentId));
    }

    // ==================================================================
    // Consultas
    // ==================================================================

    @Transactional(readOnly = true)
    public PageResponse<EmployeeSummaryResponse> search(String name,
                                                        EmploymentStatus status,
                                                        Long departmentId,
                                                        Long unitId,
                                                        LocalDate hiredFrom,
                                                        LocalDate hiredUntil,
                                                        Boolean onlyOpen,
                                                        Pageable pageable) {

        Specification<Employment> specification = EmploymentSpecifications.combine(
                EmploymentSpecifications.nameContains(name),
                EmploymentSpecifications.hasStatus(status),
                EmploymentSpecifications.inDepartment(departmentId),
                EmploymentSpecifications.inUnit(unitId),
                EmploymentSpecifications.hiredFrom(hiredFrom),
                EmploymentSpecifications.hiredUntil(hiredUntil),
                EmploymentSpecifications.onlyOpen(onlyOpen),
                EmploymentSpecifications.fetchReferences());

        Page<Employment> page = employmentRepository.findAll(specification, pageable);
        return PageResponse.from(page, EmploymentMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse findById(Long employmentId) {
        Employment employment = employmentRepository.findByIdWithDetails(employmentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Colaborador", employmentId));
        return EmploymentMapper.toResponse(employment, countEmployments(employment.getPerson().getId()));
    }

    /**
     * O vinculo em aberto de um CPF.
     *
     * <p>Util para o gestor conferir, antes de admitir, se a pessoa ja esta na
     * empresa. Aceita o CPF com ou sem pontuacao.
     */
    @Transactional(readOnly = true)
    public EmployeeResponse findOpenByCpf(String cpf) {
        String normalized = CpfValidator.normalize(cpf);
        Person person = personRepository.findByCpf(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhuma pessoa cadastrada com o CPF informado."));

        Employment employment = employmentRepository
                .findByPersonIdAndStatusNot(person.getId(), EmploymentStatus.TERMINATED)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Esta pessoa nao possui vinculo ativo. Consulte /api/v1/persons/by-cpf/{cpf} para o historico."));

        return EmploymentMapper.toResponse(employment, countEmployments(person.getId()));
    }

    // ==================================================================
    // RF11 (parcial) -- timeline
    // ==================================================================

    /**
     * Linha do tempo de um vinculo.
     *
     * @param includeProfileUpdates quando falso, esconde as edicoes cadastrais e
     *                              deixa so os marcos de carreira -- e a visao
     *                              que o gestor costuma querer
     */
    @Transactional(readOnly = true)
    public List<EmploymentEventResponse> timeline(Long employmentId, boolean includeProfileUpdates) {
        // Garante 404 coerente quando o vinculo nao existe, em vez de lista vazia.
        findEntity(employmentId);

        List<EmploymentEvent> events = includeProfileUpdates
                ? eventRepository.findByEmploymentIdOrderByEffectiveDateDescIdDesc(employmentId)
                : eventRepository.findByEmploymentIdAndEventTypeInOrderByEffectiveDateDescIdDesc(
                        employmentId,
                        Arrays.stream(EmploymentEventType.values())
                                .filter(EmploymentEventType::isCareerEvent)
                                .toList());

        return events.stream().map(EmploymentMapper::toEventResponse).toList();
    }

    // ==================================================================
    // Auxiliares
    // ==================================================================

    private Employment findEntity(Long id) {
        return employmentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Colaborador", id));
    }

    private int countEmployments(Long personId) {
        return employmentRepository.findByPersonIdOrderByHireDateDescIdDesc(personId).size();
    }

    private void applyPersonData(Person person, String rg, String fullName, LocalDate birthDate,
                                 String personalEmail, Gender gender, String phone,
                                 Integer dependentsCount, String driverLicenseCategories) {
        person.setRg(rg);
        person.setFullName(fullName.trim());
        person.setBirthDate(birthDate);
        person.setPersonalEmail(normalizeEmail(personalEmail));
        person.setGender(gender);
        person.setPhone(phone);
        person.setDependentsCount(dependentsCount == null ? 0 : dependentsCount);
        person.setDriverLicenseCategories(
                driverLicenseCategories == null ? null : driverLicenseCategories.toUpperCase(Locale.ROOT));
    }

    private void applyEmploymentData(Employment employment, String registrationNumber, String corporateEmail,
                                     LocalDate hireDate, BigDecimal salary, BigDecimal bonus,
                                     BigDecimal weeklyHours, String grade, String band,
                                     Long departmentId, Long jobPositionId, Long unitId, String notes) {
        employment.setRegistrationNumber(blankToNull(registrationNumber));
        employment.setCorporateEmail(normalizeEmail(corporateEmail));
        employment.setHireDate(hireDate);
        // Normaliza a escala antes de gravar. As colunas sao NUMERIC(12,2), entao
        // o banco arredondaria de qualquer forma -- fazer isso aqui garante que o
        // valor na entidade, no banco e no evento da timeline sejam identicos.
        // Sem isso, enviar 7300 e comparar com os 5000.00 vindos do banco produz
        // um diff inconsistente: {"salary": "5000.00"} -> {"salary": "7300"}.
        employment.setSalary(scaled(salary, 2));
        employment.setBonus(scaled(bonus, 2));
        employment.setWeeklyHours(scaled(weeklyHours, 2));
        employment.setGrade(grade);
        employment.setBand(band);
        employment.setNotes(notes);
        employment.setDepartment(resolveDepartment(departmentId));
        employment.setJobPosition(resolveJobPosition(jobPositionId));
        employment.setUnit(resolveUnit(unitId));
    }

    /**
     * Campos do vinculo que o PUT (RF05) pode alterar.
     *
     * <p>Nao inclui salario, cargo nem area: essas tres mudancas passam
     * obrigatoriamente pelo RF10, que registra motivo e vigencia. Ter um segundo
     * caminho para altera-las deixaria a RN06 furada.
     */
    private void applyEditableEmploymentData(Employment employment, String registrationNumber,
                                             String corporateEmail, LocalDate hireDate,
                                             BigDecimal bonus, BigDecimal weeklyHours,
                                             String grade, String band, Long unitId, String notes) {
        employment.setRegistrationNumber(blankToNull(registrationNumber));
        employment.setCorporateEmail(normalizeEmail(corporateEmail));
        employment.setHireDate(hireDate);
        employment.setBonus(scaled(bonus, 2));
        employment.setWeeklyHours(scaled(weeklyHours, 2));
        employment.setGrade(grade);
        employment.setBand(band);
        employment.setNotes(notes);
        employment.setUnit(resolveUnit(unitId));
    }

    private Department resolveDepartment(Long id) {
        return id == null ? null : departmentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Area de atuacao", id));
    }

    private JobPosition resolveJobPosition(Long id) {
        return id == null ? null : jobPositionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Cargo", id));
    }

    private Unit resolveUnit(Long id) {
        return id == null ? null : unitRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Unidade", id));
    }

    private void checkRegistrationNumber(String registrationNumber, Long currentId) {
        String value = blankToNull(registrationNumber);
        if (value == null) {
            return;
        }
        boolean duplicated = currentId == null
                ? employmentRepository.existsByRegistrationNumber(value)
                : employmentRepository.existsByRegistrationNumberAndIdNot(value, currentId);
        if (duplicated) {
            throw new DuplicateResourceException("registrationNumber",
                    "Ja existe um vinculo com a matricula " + value);
        }
    }

    private void checkCorporateEmail(String corporateEmail, Long currentId) {
        String value = normalizeEmail(corporateEmail);
        if (value == null) {
            return;
        }
        boolean duplicated = currentId == null
                ? employmentRepository.existsByCorporateEmail(value)
                : employmentRepository.existsByCorporateEmailAndIdNot(value, currentId);
        if (duplicated) {
            throw new DuplicateResourceException("corporateEmail",
                    "Ja existe um vinculo com o email corporativo " + value);
        }
    }

    private static Long idOf(com.sgc.backend.domain.entity.BaseEntity entity) {
        return entity == null ? null : entity.getId();
    }

    /** Ajusta a escala decimal, preservando null. */
    private static BigDecimal scaled(BigDecimal value, int scale) {
        return value == null ? null : value.setScale(scale, RoundingMode.HALF_UP);
    }

    /** Compara valores monetarios por valor, ignorando diferenca de escala. */
    private static boolean sameAmount(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.compareTo(b) == 0;
    }

    /** Campos cadastrais acompanhados pelo evento PROFILE_UPDATE. */
    private static Map<String, Object> profileSnapshot(Person person, Employment employment) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("fullName", text(person.getFullName()));
        snapshot.put("rg", text(person.getRg()));
        snapshot.put("birthDate", text(person.getBirthDate()));
        snapshot.put("personalEmail", text(person.getPersonalEmail()));
        snapshot.put("gender", text(person.getGender()));
        snapshot.put("phone", text(person.getPhone()));
        snapshot.put("dependentsCount", text(person.getDependentsCount()));
        snapshot.put("driverLicenseCategories", text(person.getDriverLicenseCategories()));
        snapshot.put("registrationNumber", text(employment.getRegistrationNumber()));
        snapshot.put("corporateEmail", text(employment.getCorporateEmail()));
        snapshot.put("grade", text(employment.getGrade()));
        snapshot.put("band", text(employment.getBand()));
        snapshot.put("bonus", text(employment.getBonus()));
        snapshot.put("weeklyHours", text(employment.getWeeklyHours()));
        snapshot.put("notes", text(employment.getNotes()));
        return snapshot;
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String normalizeEmail(String email) {
        String value = blankToNull(email);
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    /**
     * Converte string vazia em null. Necessario porque matricula e email
     * corporativo tem unique constraint: varias strings vazias colidiriam,
     * enquanto varios NULL sao aceitos pelo Postgres.
     */
    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Diferenca entre dois retratos do cadastro: guarda so as chaves que mudaram.
     *
     * @param previous valores antigos dos campos alterados
     * @param current  valores novos dos mesmos campos
     */
    private record Diff(Map<String, Object> previous, Map<String, Object> current) {

        static Diff between(Map<String, Object> before, Map<String, Object> after) {
            Map<String, Object> previous = new LinkedHashMap<>();
            Map<String, Object> current = new LinkedHashMap<>();

            for (String key : new ArrayList<>(after.keySet())) {
                Object oldValue = before.get(key);
                Object newValue = after.get(key);
                if (!Objects.equals(oldValue, newValue)) {
                    previous.put(key, oldValue);
                    current.put(key, newValue);
                }
            }
            return new Diff(
                    previous.isEmpty() ? null : previous,
                    current.isEmpty() ? null : current);
        }
    }
}
