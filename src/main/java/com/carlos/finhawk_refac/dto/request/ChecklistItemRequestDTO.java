package com.carlos.finhawk_refac.dto.request;

import java.math.BigDecimal;

public record ChecklistItemRequestDTO(
        String description,
        Integer dueDay,
        Boolean active,
        Long accountId,
        BigDecimal approximateValue
) {
}