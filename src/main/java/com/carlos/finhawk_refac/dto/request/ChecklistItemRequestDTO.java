package com.carlos.finhawk_refac.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// description/dueDay sem @NotNull/@NotBlank de proposito: update() trata
// campo nulo/ausente como "nao alterar" (atualizacao parcial). A
// obrigatoriedade em CREATE fica por conta do service.
public record ChecklistItemRequestDTO(
        @Size(max = 255, message = "A descrição pode ter no máximo 255 caracteres.")
        String description,

        @Min(value = 1, message = "O dia do vencimento deve ser entre 1 e 31.")
        @Max(value = 31, message = "O dia do vencimento deve ser entre 1 e 31.")
        Integer dueDay,

        Boolean active,

        Long accountId,

        @DecimalMin(value = "0.00", message = "O valor aproximado não pode ser negativo.")
        @DecimalMax(value = "999999999.99", message = "O valor informado é grande demais.")
        BigDecimal approximateValue
) {
}
