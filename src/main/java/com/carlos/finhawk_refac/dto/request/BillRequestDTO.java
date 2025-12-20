package com.carlos.finhawk_refac.dto.request;

import com.carlos.finhawk_refac.enums.Periodicity;
import com.carlos.finhawk_refac.enums.StatusBill;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BillRequestDTO(
        String description,
        LocalDate emission,
        LocalDate maturity,
        BigDecimal totalAmount,
        Integer installmentCount,
        Periodicity periodicity,
        StatusBill status,
        Long categoryId
) {
}