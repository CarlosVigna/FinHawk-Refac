package com.carlos.finhawk_refac.dto.request;

public record UserAccountRequestDTO(
        String name,
        String email,
        String password
) {
}
