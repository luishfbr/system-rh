package com.sgc.backend.dto.response;

import com.sgc.backend.domain.enums.EmploymentEventType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/**
 * Um fato na linha do tempo (RF11).
 *
 * @param effectiveDate quando o fato passou a valer -- e por esta data que a
 *                      timeline se ordena
 * @param recordedAt    quando foi lancado no sistema; difere de
 *                      {@code effectiveDate} em lancamentos retroativos
 * @param reason        motivo, quando o tipo de evento tem um. O vocabulario
 *                      depende do tipo: TERMINATION usa TerminationReason,
 *                      SALARY_CHANGE usa ChangeReason
 * @param changeId      a alteracao programada (RF10) que originou o evento, se houver
 * @param previousValue estado anterior aos campos alterados (null em HIRE)
 * @param newValue      estado posterior
 */
public record EmploymentEventResponse(
        Long id,
        EmploymentEventType eventType,
        LocalDate effectiveDate,
        String reason,
        String notes,
        Map<String, Object> previousValue,
        Map<String, Object> newValue,
        Long changeId,
        String performedBy,
        Instant recordedAt
) {
}
