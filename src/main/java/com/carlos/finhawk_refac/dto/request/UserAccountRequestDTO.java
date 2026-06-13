package com.carlos.finhawk_refac.dto.request;

import jakarta.validation.constraints.Size;

public record UserAccountRequestDTO(
        String name,
        String email,
        @Size(min = 6, message = "Password must have at least 6 characters")
        String password
) {
}
