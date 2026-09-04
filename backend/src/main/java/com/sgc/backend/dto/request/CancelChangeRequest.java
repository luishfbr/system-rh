package com.sgc.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cancelamento de uma alteracao ainda pendente (RF10).
 *
 * <p>O motivo e obrigatorio: a alteracao cancelada permanece no historico, e sem
 * justificativa ela viraria um registro inexplicavel para quem consultar depois.
 */
public record CancelChangeRequest(

        @Schema(example = "Promocao adiada para o proximo ciclo")
        @NotBlank(message = "motivo do cancelamento e obrigatorio")
        @Size(max = 1000)
        String reason
) {
}
