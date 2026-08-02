package com.carlos.finhawk_refac.dto.request;

import com.carlos.finhawk_refac.enums.AgendaCompletionStatus;

import java.time.LocalDate;

public record AgendaEventCompletionRequestDTO(
        LocalDate eventDate,
        AgendaCompletionStatus status
) {
}
