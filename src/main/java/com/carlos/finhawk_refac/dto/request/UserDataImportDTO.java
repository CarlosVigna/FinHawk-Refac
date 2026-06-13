package com.carlos.finhawk_refac.dto.request;

import com.carlos.finhawk_refac.enums.StatusBill;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserDataImportDTO(
        String version,
        List<AccountImportData> accounts,
        List<CategoryImportData> categories,
        List<BillImportData> bills,
        List<ChecklistItemImportData> checklistItems
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AccountImportData(Long id, String name, String photoUrl) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CategoryImportData(Long id, String name, String type, Long accountId) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BillImportData(
            Long id,
            String description,
            LocalDate emission,
            LocalDate maturity,
            BigDecimal installmentAmount,
            Integer installmentCount,
            Integer currentInstallment,
            StatusBill status,
            Long accountId,
            CategoryImportData category,
            String periodicity,
            LocalDateTime paidAt,
            LocalDateTime receivedAt
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChecklistItemImportData(Long id, String description, Integer dueDay, Boolean active, Long accountId) {}
}
