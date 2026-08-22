package com.carlos.finhawk_refac.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

// Espelha o formato de finhawk_import_*.json (snake_case, vindo de
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

    // date fica String (formato ISO "yyyy-MM-dd", igual ao JSON de origem) em
    // vez de LocalDate de proposito -- jackson-datatype-jsr310 nao esta no
    // classpath de compilacao deste projeto, entao o parse pra LocalDate e
    // feito manualmente em AdminImportService (LocalDate.parse), evitando
    // adicionar uma dependencia nova so pra um endpoint temporario.
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BillImportItem(
            String description,
            String date,
            BigDecimal amount,
            @JsonProperty("category_name") String categoryName,
            @JsonProperty("category_type") String categoryType,
            String status
    ) {}
}
