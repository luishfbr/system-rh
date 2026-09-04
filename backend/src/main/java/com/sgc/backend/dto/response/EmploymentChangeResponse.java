package com.sgc.backend.dto.response;

import com.sgc.backend.domain.enums.ChangeReason;
import com.sgc.backend.domain.enums.ChangeStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Uma alteracao de carreira (RF10).
 *
 * <p>Os campos {@code previous*} so vem preenchidos depois da aplicacao: antes
 * disso ainda nao se sabe qual sera o estado anterior, porque ele e capturado no
 * momento em que a alteracao passa a valer.
 *
 * @param daysUntilEffective dias que faltam para a vigencia; negativo indica
 *                           alteracao retroativa, nulo quando ja aplicada
 */
public record EmploymentChangeResponse(
        Long id,
        Long employmentId,
        String employeeName,

        LocalDate effectiveDate,
        ChangeStatus status,
        ChangeReason reason,
        String notes,

        BigDecimal previousSalary,
        BigDecimal newSalary,
        ReferenceResponse previousJobPosition,
        ReferenceResponse newJobPosition,
        ReferenceResponse previousDepartment,
        ReferenceResponse newDepartment,

        Integer daysUntilEffective,

        Instant appliedAt,
        Instant cancelledAt,
        String cancellationReason,

        Instant createdAt,
        String createdBy
) {
}
