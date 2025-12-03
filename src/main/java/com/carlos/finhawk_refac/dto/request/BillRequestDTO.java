package com.carlos.finhawk_refac.dto.request;

import com.carlos.finhawk_refac.entity.Category;
import com.carlos.finhawk_refac.enums.Periodicity;
import com.carlos.finhawk_refac.enums.StatusBill;

import java.time.LocalDate;

public record BillRequestDTO(
        String description,
        LocalDate maturity,
        LocalDate emission,
        Integer installmentAmount,
        Integer parcelNumber,
        Periodicity periodicity,
        StatusBill status,
        Long categoryId,
        Long accountId
) {
}
