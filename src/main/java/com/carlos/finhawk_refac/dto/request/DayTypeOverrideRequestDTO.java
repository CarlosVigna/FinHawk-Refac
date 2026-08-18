package com.carlos.finhawk_refac.dto.request;

import com.carlos.finhawk_refac.enums.DayType;

import java.time.LocalDate;

public record DayTypeOverrideRequestDTO(LocalDate date, DayType dayType) {
}
