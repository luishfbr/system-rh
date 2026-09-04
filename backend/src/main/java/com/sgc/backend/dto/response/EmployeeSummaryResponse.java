package com.sgc.backend.dto.response;

import com.sgc.backend.domain.enums.EmploymentStatus;

import java.time.LocalDate;

/**
 * Versao enxuta do vinculo, para listagens.
 *
 * <p>Separada de {@code EmployeeResponse} porque uma pagina de 20 registros nao
 * deve trafegar salario, endereco e observacoes -- menos dados na rede (RNF05)
 * e menos exposicao de informacao sensivel (RNF06).
 */
public record EmployeeSummaryResponse(
        Long id,
        Long personId,
        String cpf,
        String fullName,
        String registrationNumber,
        String corporateEmail,
        EmploymentStatus status,
        LocalDate hireDate,
        LocalDate terminationDate,
        String department,
        String jobPosition,
        String unit
) {
}
