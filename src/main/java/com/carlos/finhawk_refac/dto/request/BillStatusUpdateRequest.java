package com.carlos.finhawk_refac.dto.request;

import com.carlos.finhawk_refac.enums.StatusBill;

public record BillStatusUpdateRequest(
        StatusBill status
) {
}
