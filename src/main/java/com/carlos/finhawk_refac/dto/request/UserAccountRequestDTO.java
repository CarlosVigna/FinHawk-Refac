package com.carlos.finhawk_refac.dto.request;

import com.carlos.finhawk_refac.enums.UserRole;

public record UserAccountRequestDTO(
        String name,
        String email,
        String password,
        UserRole role
) {
}
