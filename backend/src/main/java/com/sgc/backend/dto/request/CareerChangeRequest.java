package com.sgc.backend.dto.request;

import com.sgc.backend.domain.enums.ChangeReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Alteracao de cargo, salario ou setor (RF10).
 *
 * <p>Os tres campos de destino sao opcionais, mas <b>ao menos um</b> deve vir
 * preenchido -- uma alteracao que nao altera nada nao faz sentido. Deixar os tres
 * na mesma requisicao e proposital: uma promocao normalmente muda cargo e salario
 * de uma vez, e o certo e que isso seja um unico lancamento, com uma unica data
 * de vigencia e um unico motivo.
 *
 * @param effectiveDate quando passa a valer. No passado ou hoje, aplica na hora;
 *                      no futuro, fica pendente ate o job diario alcanca-la
 */
public record CareerChangeRequest(

        @Schema(example = "2026-10-01", description = "Pode ser retroativa ou futura")
        @NotNull(message = "data de vigencia e obrigatoria")
        LocalDate effectiveDate,

        @Schema(example = "PROMOTION")
        @NotNull(message = "motivo da alteracao e obrigatorio")
        ChangeReason reason,

        @Schema(example = "9200.00", description = "Novo salario. Omita para nao alterar.")
        @DecimalMin(value = "0.0", message = "salario nao pode ser negativo")
        BigDecimal salary,

        @Schema(description = "Id do novo cargo. Omita para nao alterar.")
        Long jobPositionId,

        @Schema(description = "Id da nova area de atuacao. Omita para nao alterar.")
        Long departmentId,

        @Schema(example = "Promocao aprovada no ciclo de avaliacao 2026/1")
        @Size(max = 1000)
        String notes
) {

    /** Ao menos um destino precisa vir preenchido -- validado tambem por CHECK no banco. */
    public boolean hasAnyTarget() {
        return salary != null || jobPositionId != null || departmentId != null;
    }
}
