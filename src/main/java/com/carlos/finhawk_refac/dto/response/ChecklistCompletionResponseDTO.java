package com.carlos.finhawk_refac.dto.response;

import java.time.LocalDateTime;

public record ChecklistCompletionResponseDTO(
        Long id,
        Long checklistItemId,
        String month,
        LocalDateTime completedAt
) {
}
