package com.carlos.finhawk_refac.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountRequestDTO(
        @NotBlank(message = "O nome da conta é obrigatório.")
        @Size(max = 255, message = "O nome pode ter no máximo 255 caracteres.")
        String name,

        @Size(max = 255, message = "A URL da foto é grande demais.")
        String photoUrl
) {
}
