package com.carlos.finhawk_refac.dto.response;

import java.math.BigDecimal;

public record ChecklistItemResponseDTO(
        Long id,
        String description,
        Integer dueDay,
        Boolean active,
        Long accountId,
        BigDecimal approximateValue
) {
}