package com.carlos.finhawk_refac.dto.response;

import com.carlos.finhawk_refac.enums.AgendaEventType;
import com.carlos.finhawk_refac.enums.RecurrenceFrequency;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record AgendaEventResponseDTO(
        Long id,
        String title,
        String description,
        Long accountId,
        AgendaEventType type,
        LocalDateTime eventDateTime,
        RecurrenceFrequency recurrenceFrequency,
        List<DayOfWeek> daysOfWeek,
        LocalTime timeOfDay,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
