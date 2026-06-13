package com.carlos.finhawk_refac.dto.response;

import com.carlos.finhawk_refac.enums.Periodicity;
import com.carlos.finhawk_refac.enums.StatusBill;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BillResponseDTO(
        Long id,
        String description,
        LocalDate emission,
        LocalDate maturity,
        BigDecimal installmentAmount,
        Integer installmentCount,
        Integer currentInstallment,
        StatusBill status,
        Periodicity periodicity,
        Long accountId,
        CategoryResponseDTO category
        ,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime paidAt,
        LocalDateTime receivedAt
) {
}