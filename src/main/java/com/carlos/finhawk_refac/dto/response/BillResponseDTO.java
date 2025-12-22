package com.carlos.finhawk_refac.dto.response;

import com.carlos.finhawk_refac.enums.Periodicity;
import com.carlos.finhawk_refac.enums.StatusBill;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BillResponseDTO(
        Long id,
        String description,
        LocalDate emission,
        LocalDate maturity,
        BigDecimal installmentAmount,
        Integer installmentCount,
        Integer currentInstallment,
        Periodicity periodicity,
        StatusBill status,
        String categoryName
) {
}