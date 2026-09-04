package com.sgc.backend.mapper;

import com.sgc.backend.domain.entity.Department;
import com.sgc.backend.domain.entity.Employment;
import com.sgc.backend.domain.entity.EmploymentChange;
import com.sgc.backend.domain.entity.EmploymentEvent;
import com.sgc.backend.domain.entity.JobPosition;
import com.sgc.backend.domain.entity.Person;
import com.sgc.backend.domain.entity.Unit;
import com.sgc.backend.dto.response.EmployeeResponse;
import com.sgc.backend.dto.response.EmployeeSummaryResponse;
import com.sgc.backend.dto.response.EmploymentChangeResponse;
import com.sgc.backend.dto.response.EmploymentEventResponse;
import com.sgc.backend.dto.response.EmploymentSummaryResponse;
import com.sgc.backend.dto.response.ReferenceResponse;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Conversao dos vinculos e da timeline.
 *
 * <p>Escrito a mao, sem MapStruct nem ModelMapper: o codigo abaixo e exatamente
 * o que roda. Em troca da verbosidade, fica evidente onde a pessoa e o vinculo
 * se juntam para formar o "colaborador" que a API expoe.
 */
public final class EmploymentMapper {

    private EmploymentMapper() {
    }

    /**
     * Achata pessoa + vinculo na resposta completa.
     *
     * @param employmentCount quantos periodos a pessoa ja teve; maior que 1
     *                        sinaliza readmissao
     */
    public static EmployeeResponse toResponse(Employment employment, int employmentCount) {
        if (employment == null) {
            return null;
        }
        Person person = employment.getPerson();

        return new EmployeeResponse(
                employment.getId(),
                person.getId(),

                PersonMapper.maskCpf(person.getCpf()),
                person.getRg(),
                person.getFullName(),
                person.getBirthDate(),
                PersonMapper.calculateAge(person.getBirthDate()),
                person.getPersonalEmail(),
                person.getGender(),
                person.getPhone(),
                person.getDependentsCount(),
                person.getDriverLicenseCategories(),
                PersonMapper.toAddressResponse(person.getAddress()),

                employment.getRegistrationNumber(),
                employment.getCorporateEmail(),
                employment.getHireDate(),
                employment.getTerminationDate(),
                employment.getTerminationReason(),
                employment.getTerminationNotes(),
                employment.getStatus(),
                employment.getSalary(),
                employment.getBonus(),
                employment.getWeeklyHours(),
                employment.getGrade(),
                employment.getBand(),

                toReference(employment.getDepartment()),
                toReference(employment.getJobPosition()),
                toReference(employment.getUnit()),

                employment.getNotes(),
                employmentCount,

                employment.getCreatedAt(),
                employment.getUpdatedAt(),
                employment.getCreatedBy(),
                employment.getUpdatedBy());
    }

    public static EmployeeSummaryResponse toSummary(Employment employment) {
        if (employment == null) {
            return null;
        }
        Person person = employment.getPerson();

        return new EmployeeSummaryResponse(
                employment.getId(),
                person.getId(),
                PersonMapper.maskCpf(person.getCpf()),
                person.getFullName(),
                employment.getRegistrationNumber(),
                employment.getCorporateEmail(),
                employment.getStatus(),
                employment.getHireDate(),
                employment.getTerminationDate(),
                employment.getDepartment() == null ? null : employment.getDepartment().getName(),
                employment.getJobPosition() == null ? null : employment.getJobPosition().getTitle(),
                employment.getUnit() == null ? null : employment.getUnit().getName());
    }

    /** Resumo sem dados pessoais, para a lista de periodos dentro de uma pessoa. */
    public static EmploymentSummaryResponse toEmploymentSummary(Employment employment) {
        if (employment == null) {
            return null;
        }
        return new EmploymentSummaryResponse(
                employment.getId(),
                employment.getRegistrationNumber(),
                employment.getHireDate(),
                employment.getTerminationDate(),
                employment.getStatus(),
                employment.getTerminationReason(),
                toReference(employment.getDepartment()),
                toReference(employment.getJobPosition()),
                toReference(employment.getUnit()));
    }

    public static EmploymentEventResponse toEventResponse(EmploymentEvent event) {
        if (event == null) {
            return null;
        }
        return new EmploymentEventResponse(
                event.getId(),
                event.getEventType(),
                event.getEffectiveDate(),
                event.getReason(),
                event.getNotes(),
                event.getPreviousValue(),
                event.getNewValue(),
                event.getEmploymentChange() == null ? null : event.getEmploymentChange().getId(),
                event.getPerformedBy(),
                event.getCreatedAt());
    }

    /**
     * Converte uma alteracao de carreira (RF10).
     *
     * <p>{@code daysUntilEffective} e calculado na leitura, nunca armazenado:
     * "faltam 27 dias" e uma verdade que muda a cada amanhecer.
     */
    public static EmploymentChangeResponse toChangeResponse(EmploymentChange change) {
        if (change == null) {
            return null;
        }
        Employment employment = change.getEmployment();

        Integer daysUntilEffective = change.getStatus().isPending()
                ? (int) ChronoUnit.DAYS.between(LocalDate.now(), change.getEffectiveDate())
                : null;

        return new EmploymentChangeResponse(
                change.getId(),
                employment.getId(),
                employment.getPerson().getFullName(),

                change.getEffectiveDate(),
                change.getStatus(),
                change.getReason(),
                change.getNotes(),

                change.getPreviousSalary(),
                change.getNewSalary(),
                toReference(change.getPreviousJobPosition()),
                toReference(change.getNewJobPosition()),
                toReference(change.getPreviousDepartment()),
                toReference(change.getNewDepartment()),

                daysUntilEffective,

                change.getAppliedAt(),
                change.getCancelledAt(),
                change.getCancellationReason(),

                change.getCreatedAt(),
                change.getCreatedBy());
    }

    // ------------------------------------------------------------------

    private static ReferenceResponse toReference(Department department) {
        return department == null ? null : new ReferenceResponse(department.getId(), department.getName());
    }

    private static ReferenceResponse toReference(JobPosition jobPosition) {
        return jobPosition == null ? null : new ReferenceResponse(jobPosition.getId(), jobPosition.getTitle());
    }

    private static ReferenceResponse toReference(Unit unit) {
        return unit == null ? null : new ReferenceResponse(unit.getId(), unit.getName());
    }
}
