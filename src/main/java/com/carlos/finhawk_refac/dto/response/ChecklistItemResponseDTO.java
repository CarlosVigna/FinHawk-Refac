package com.carlos.finhawk_refac.dto.response;

public record ChecklistItemResponseDTO(
        Long id,
        String description,
        Integer dueDay,
        Boolean active,
        Long accountId
) {
}