package com.carlos.finhawk_refac.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record UserDataExportDTO(
        LocalDateTime exportedAt,
        String version,
        UserExportDTO user,
        List<AccountResponseDTO> accounts,
        List<CategoryExportDTO> categories,
        List<BillResponseDTO> bills,
        List<ChecklistItemResponseDTO> checklistItems
) {
    public record UserExportDTO(Long id, String name, String email) {}
    public record CategoryExportDTO(Long id, String name, String type, Long accountId) {}
}
