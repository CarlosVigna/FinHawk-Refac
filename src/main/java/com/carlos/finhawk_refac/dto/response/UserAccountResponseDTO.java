package com.carlos.finhawk_refac.dto.response;


public record UserAccountResponseDTO(
        Long id,
        String name,
        String email,
        String role
) {
}
