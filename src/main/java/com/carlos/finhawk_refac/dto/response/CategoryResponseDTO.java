package com.carlos.finhawk_refac.dto.response;

import com.carlos.finhawk_refac.enums.CategoryType;

public record CategoryResponseDTO(
        Long id,
        String name,
        CategoryType type
) {
}
