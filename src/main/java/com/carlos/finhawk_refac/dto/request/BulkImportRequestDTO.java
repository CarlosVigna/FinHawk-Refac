package com.carlos.finhawk_refac.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Espelha o formato de finhawk_import_payload*.json (snake_case, vindo de
// planilha) -- usado uma unica vez pelo AdminImportController pra carregar
// o historico categorizado manualmente de extratos bancarios.
@JsonIgnoreProperties(ignoreUnknown = true)
public record BulkImportRequestDTO(
        @JsonProperty("account_id") Long accountId,
        @JsonProperty("user_email") String userEmail,
        List<CategoryImportItem> categories,
        List<BillImportItem> bills
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CategoryImportItem(String name, String type) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BillImportItem(
            String description,
            LocalDate date,
            BigDecimal amount,
            @JsonProperty("category_name") String categoryName,
            @JsonProperty("category_type") String categoryType,
            String status
    ) {}
}
