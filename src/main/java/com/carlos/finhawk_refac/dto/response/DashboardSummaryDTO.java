package com.carlos.finhawk_refac.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryDTO(
        BigDecimal patrimonioConsolidado,
        Integer accountCount,
        List<AccountSummaryDTO> accounts
) {
}

