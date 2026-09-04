package com.sgc.backend.dto.request;

import com.sgc.backend.domain.enums.TerminationReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Desligamento de um colaborador (RF08).
 *
 * <p>O motivo e obrigatorio e vem de uma lista fechada -- e ele que torna o
 * relatorio de desligamentos do RF07 agrupavel. O texto livre fica em
 * {@code notes}, para o contexto que a lista nao cobre.
 *
 * @param terminationDate data do desligamento; pode ser retroativa, mas nunca
 *                        anterior a admissao (validado no service e por CHECK no banco)
 */
public record TerminateEmploymentRequest(

        @Schema(example = "2026-09-30")
        @NotNull(message = "data de desligamento e obrigatoria")
        LocalDate terminationDate,

        @Schema(example = "RESIGNATION")
        @NotNull(message = "motivo do desligamento e obrigatorio")
        TerminationReason reason,

        @Schema(example = "Recebeu proposta externa")
        @Size(max = 1000)
        String notes
) {
}
