package com.carlos.finhawk_refac.dto.request;

import java.math.BigDecimal;

public record ResetAndReimportRequestDTO(
        Long accountId,
        BigDecimal targetBalance
) {
}
