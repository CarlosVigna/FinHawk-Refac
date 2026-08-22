package com.carlos.finhawk_refac.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// Dump completo de um Bill, todos os campos -- usado so pro backup de
// seguranca antes de uma limpeza em massa (AdminImportService), pra dar
// pra reconstruir o registro manualmente se precisar.
public record BillBackupDTO(
        Long id,
        String description,
        LocalDate emission,
        LocalDate maturity,
        BigDecimal installmentAmount,
        Integer installmentCount,
        Integer currentInstallment,
        String periodicity,
        String status,
        String categoryName,
        String categoryType,
        Long accountId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime paidAt,
        LocalDateTime receivedAt
) {
}
