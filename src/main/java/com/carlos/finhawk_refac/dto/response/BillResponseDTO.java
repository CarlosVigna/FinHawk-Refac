package com.carlos.finhawk_refac.dto.response;

import com.carlos.finhawk_refac.enums.Periodicity;
import com.carlos.finhawk_refac.enums.StatusBill;

import java.time.LocalDate;
public record BillResponseDTO(
        Long id,
        String description,
        LocalDate maturity,
        LocalDate emission,
        Integer installmentAmount,
        Integer parcelNumber,
        Periodicity periodicity,
        StatusBill status,
        String categoryName
) {
}
