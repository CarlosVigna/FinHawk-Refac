package com.carlos.finhawk_refac.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// name/type ficam sem @NotBlank de proposito: CategoryService.update() trata
// campo nulo como "nao alterar" (atualizacao parcial). @Size/@Pattern nao
// rejeitam null, entao continuam validando quando o valor e enviado sem
// quebrar esse contrato. A obrigatoriedade em CREATE fica por conta do
// service, que ja rejeita null/branco antes de usar o valor.
public record CategoryRequestDTO(
        @Size(max = 255, message = "O nome pode ter no máximo 255 caracteres.")
        String name,

        @Pattern(regexp = "(?i)RECEIPT|PAYMENT", message = "Tipo inválido. Use RECEIPT ou PAYMENT.")
        String type,

        Long accountId
) {
}
