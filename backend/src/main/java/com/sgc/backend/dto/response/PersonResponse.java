package com.sgc.backend.dto.response;

import com.sgc.backend.domain.enums.Gender;

import java.time.LocalDate;
import java.util.List;

/**
 * A pessoa e todos os seus periodos de contrato.
 *
 * <p>E a visao que responde "essa pessoa ja trabalhou aqui antes?" -- consulta
 * que passou a fazer sentido quando a readmissao virou um vinculo novo em vez
 * de uma reativacao do mesmo registro.
 *
 * @param employments do periodo mais recente para o mais antigo
 */
public record PersonResponse(
        Long id,
        String cpf,
        String rg,
        String fullName,
        LocalDate birthDate,
        Integer age,
        String personalEmail,
        Gender gender,
        String phone,
        Integer dependentsCount,
        String driverLicenseCategories,
        AddressResponse address,
        List<EmploymentSummaryResponse> employments
) {
}
