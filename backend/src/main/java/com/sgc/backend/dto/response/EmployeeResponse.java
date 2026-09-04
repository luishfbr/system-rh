package com.sgc.backend.dto.response;

import com.sgc.backend.domain.enums.EmploymentStatus;
import com.sgc.backend.domain.enums.Gender;
import com.sgc.backend.domain.enums.TerminationReason;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Um colaborador na visao da API: o <b>vinculo</b>, com os dados da pessoa junto.
 *
 * <p>Achatar pessoa e vinculo numa unica resposta e proposital -- a tela de
 * detalhe mostra tudo isso lado a lado, e obrigar o frontend a duas chamadas
 * nao acrescentaria nada.
 *
 * @param id       identificador do <b>vinculo</b>; e ele que as rotas
 *                 {@code /api/v1/employees/{id}} recebem
 * @param personId identificador da pessoa; use-o para chegar aos demais periodos
 * @param cpf      mascarado ({@code ***.982.247-**}) por minimizacao de dados (RNF06)
 */
public record EmployeeResponse(
        Long id,
        Long personId,

        // ---------- Pessoa ----------
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

        // ---------- Vinculo ----------
        String registrationNumber,
        String corporateEmail,
        LocalDate hireDate,
        LocalDate terminationDate,
        TerminationReason terminationReason,
        String terminationNotes,
        EmploymentStatus status,
        BigDecimal salary,
        BigDecimal bonus,
        BigDecimal weeklyHours,
        String grade,
        String band,

        ReferenceResponse department,
        ReferenceResponse jobPosition,
        ReferenceResponse unit,

        String notes,

        /** Quantos periodos de contrato esta pessoa ja teve. Maior que 1 indica readmissao. */
        Integer employmentCount,

        // ---------- Auditoria (RN03) ----------
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy
) {
}
