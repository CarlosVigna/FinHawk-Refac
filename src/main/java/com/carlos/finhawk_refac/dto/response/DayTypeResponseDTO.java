package com.carlos.finhawk_refac.dto.response;

import com.carlos.finhawk_refac.enums.DayType;

import java.time.LocalDate;
import java.util.List;

public record DayTypeResponseDTO(LocalDate date, List<DayType> dayTypes, boolean overridden) {
}
