package com.carlos.finhawk_refac.dto.response;

import java.math.BigDecimal;

public record AccountSummaryDTO(
        Long accountId,
        String name,
        BigDecimal receitasRealizadas,
        BigDecimal despesasRealizadas,
        BigDecimal saldoRealizado
) {
}

