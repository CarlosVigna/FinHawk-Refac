package com.carlos.finhawk_refac.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ResetAndReimportResultDTO(
        int billsDeleted,
        int categoriesReused,
        int categoriesCreated,
        List<String> categoriesComFalha,
        int billsImported,
        List<String> billsComFalha,
        BigDecimal balanceBeforeAdjustment,
        BigDecimal targetBalance,
        BigDecimal adjustmentAmount,
        String adjustmentDirection,
        BigDecimal finalBalance
) {
}
