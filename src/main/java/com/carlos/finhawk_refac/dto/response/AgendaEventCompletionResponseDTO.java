package com.carlos.finhawk_refac.dto.response;

import com.carlos.finhawk_refac.enums.AgendaCompletionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AgendaEventCompletionResponseDTO(
        Long id,
        Long agendaEventId,
        LocalDate eventDate,
        AgendaCompletionStatus status,
        LocalDateTime completedAt
) {
}
