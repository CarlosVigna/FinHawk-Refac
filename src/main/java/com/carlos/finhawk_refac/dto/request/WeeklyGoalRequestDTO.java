package com.carlos.finhawk_refac.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WeeklyGoalRequestDTO(
        @NotBlank(message = "O título é obrigatório.")
        @Size(max = 255, message = "O título pode ter no máximo 255 caracteres.")
        String title,

        @Size(max = 1000, message = "A descrição pode ter no máximo 1000 caracteres.")
        String description,

        @NotNull(message = "A conta é obrigatória.")
        Long accountId
) {
}
