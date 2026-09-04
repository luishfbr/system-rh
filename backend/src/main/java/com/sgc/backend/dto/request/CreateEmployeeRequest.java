package com.sgc.backend.dto.request;

import com.sgc.backend.domain.enums.Gender;
import com.sgc.backend.validation.Cpf;
import io.swagger.v3.oas.annotations.media.Schema;
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
 * Admissao de um colaborador (RF04).
 *
 * <p>Reune o cadastro pessoal e o profissional numa unica requisicao. As regras
 * que dependem do estado do banco (CPF ja ativo -- RN07, matricula repetida)
 * sao verificadas no service; aqui ficam apenas as validacoes de formato.
 */
public record CreateEmployeeRequest(

        // ---------- Cadastro pessoal ----------
        @Schema(example = "529.982.247-25", description = "Com ou sem pontuacao")
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

        @Schema(example = "AB", description = "Categorias da CNH concatenadas")
        @Size(max = 10)
        String driverLicenseCategories,

        @Valid
        AddressRequest address,

        // ---------- Cadastro profissional ----------
        @Schema(description = "Matricula")
        @Size(max = 20)
        String registrationNumber,

        @Email(message = "email corporativo invalido")
        @Size(max = 255)
        String corporateEmail,

        @NotNull(message = "data de admissao e obrigatoria")
        LocalDate hireDate,

        @DecimalMin(value = "0.0", message = "salario nao pode ser negativo")
        BigDecimal salary,

        @Schema(description = "Gratificacao")
        @DecimalMin(value = "0.0", message = "gratificacao nao pode ser negativa")
        BigDecimal bonus,

        @Schema(description = "Carga horaria semanal")
        @DecimalMin(value = "0.0", message = "carga horaria nao pode ser negativa")
        BigDecimal weeklyHours,

        @Size(max = 10)
        String grade,

        @Schema(description = "Faixa")
        @Size(max = 10)
        String band,

        Long departmentId,

        Long jobPositionId,

        Long unitId,

        @Schema(description = "Observacoes")
        String notes
) {
}
