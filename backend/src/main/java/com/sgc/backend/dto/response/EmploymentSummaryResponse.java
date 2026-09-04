package com.sgc.backend.dto.response;

import com.sgc.backend.domain.enums.EmploymentStatus;
import com.sgc.backend.domain.enums.TerminationReason;

import java.time.LocalDate;

/**
 * Resumo de um periodo de contrato, usado ao listar o historico de uma pessoa.
 *
 * <p>Sem dados pessoais: quem consulta ja esta olhando a pessoa e nao precisa
 * ver nome e CPF repetidos em cada vinculo.
 */
public record EmploymentSummaryResponse(
        Long id,
        String registrationNumber,
        LocalDate hireDate,
        LocalDate terminationDate,
        EmploymentStatus status,
        TerminationReason terminationReason,
        ReferenceResponse department,
        ReferenceResponse jobPosition,
        ReferenceResponse unit
) {
}
