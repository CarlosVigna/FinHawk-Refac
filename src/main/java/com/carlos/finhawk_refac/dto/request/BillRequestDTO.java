package com.carlos.finhawk_refac.dto.request;

import com.carlos.finhawk_refac.enums.Periodicity;
import com.carlos.finhawk_refac.enums.StatusBill;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BillRequestDTO(
        @NotBlank(message = "A descrição é obrigatória.")
        @Size(max = 255, message = "A descrição pode ter no máximo 255 caracteres.")
        String description,

        @NotNull(message = "A data de emissão é obrigatória.")
        LocalDate emission,

        @NotNull(message = "A data de vencimento é obrigatória.")
        LocalDate maturity,

        @NotNull(message = "O valor é obrigatório.")
        @DecimalMin(value = "0.00", message = "O valor não pode ser negativo.")
        @DecimalMax(value = "999999999.99", message = "O valor informado é grande demais.")
        BigDecimal installmentAmount,

        @Max(value = 360, message = "Número de parcelas grande demais (máximo 360).")
        Integer installmentCount,

        @NotNull(message = "A periodicidade é obrigatória.")
        Periodicity periodicity,

        StatusBill status,

        @NotNull(message = "A categoria é obrigatória.")
        Long categoryId,

        @NotNull(message = "A conta é obrigatória.")
        Long accountId
) {
}
