package com.carlos.finhawk_refac.dto.request;

import com.carlos.finhawk_refac.enums.CategoryType;

public record CategoryRequestDTO(
        String name,
        CategoryType type
) {
}
