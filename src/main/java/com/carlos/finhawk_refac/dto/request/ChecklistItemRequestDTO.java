package com.carlos.finhawk_refac.dto.request;

public record ChecklistItemRequestDTO(
        String description,
        Integer dueDay,
        Boolean active,
        Long accountId
) {
}