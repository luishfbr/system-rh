package com.sgc.backend.dto.request;

import com.sgc.backend.domain.enums.Gender;
import com.sgc.backend.validation.Cpf;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Edicao de um colaborador (RF05).
 *
 * <p><b>Nao inclui salario, cargo nem area de atuacao.</b> Essas tres mudancas
 * passaram a ser exclusivas do RF10
 * ({@code PATCH /employees/{id}/career-changes}), que exige motivo e data de
 * vigencia. Permitir os dois caminhos deixaria a RN06 furada: seria possivel
 * alterar um salario sem dizer por que, e sem registrar na timeline.
 *
 * <p>Tambem nao inclui {@code status} nem {@code terminationDate}: mudanca de
 * situacao acontece por endpoints dedicados (inativacao aqui; desligamento no
 * RF08), para que cada transicao gere seu proprio registro.
 */
public record UpdateEmployeeRequest(

        @NotBlank(message = "CPF e obrigatorio")
        @Cpf
        String cpf,

        @Size(max = 20)
        String rg,

        @NotBlank(message = "nome completo e obrigatorio")
        @Size(max = 150)
        String fullName,

        @NotNull(message = "data de nascimento e obrigatoria")
        @Past(message = "data de nascimento deve estar no passado")
        LocalDate birthDate,

        @Email(message = "email pessoal invalido")
        @Size(max = 255)
        String personalEmail,

        Gender gender,

        @Size(max = 20)
        String phone,

        @PositiveOrZero(message = "quantidade de dependentes nao pode ser negativa")
        Integer dependentsCount,

        @Size(max = 10)
        String driverLicenseCategories,

        @Valid
        AddressRequest address,

        @Size(max = 20)
        String registrationNumber,

        @Email(message = "email corporativo invalido")
        @Size(max = 255)
        String corporateEmail,

        @NotNull(message = "data de admissao e obrigatoria")
        LocalDate hireDate,

        @DecimalMin(value = "0.0", message = "gratificacao nao pode ser negativa")
        BigDecimal bonus,

        @DecimalMin(value = "0.0", message = "carga horaria nao pode ser negativa")
        BigDecimal weeklyHours,

        @Size(max = 10)
        String grade,

        @Size(max = 10)
        String band,

        Long unitId,

        String notes
) {
}
