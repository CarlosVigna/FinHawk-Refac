package com.carlos.finhawk_refac.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record WeeklyGoalResponseDTO(
        Long id,
        String title,
        String description,
        Long accountId,
        LocalDate weekStartDate,
        Boolean completed,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
