package com.carlos.finhawk_refac.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ChecklistSuggestionDTO(
        BigDecimal lastAmount,
        Long lastCategoryId,
        LocalDate lastLaunchedAt,
        String lastDescription
) {
}

