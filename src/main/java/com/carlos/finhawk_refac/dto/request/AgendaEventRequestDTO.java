package com.carlos.finhawk_refac.dto.request;

import com.carlos.finhawk_refac.enums.AgendaEventType;
import com.carlos.finhawk_refac.enums.DayType;
import com.carlos.finhawk_refac.enums.RecurrenceFrequency;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

// Campos sem @NotNull de proposito: update() trata campo ausente como
// "nao alterar" (atualizacao parcial). A obrigatoriedade em CREATE, e a
// validacao cruzada entre campos especificos de ONE_TIME/HABIT, ficam
// por conta do service (dependem do valor de `type`).
public record AgendaEventRequestDTO(
        @Size(max = 255, message = "O título pode ter no máximo 255 caracteres.")
        String title,

        @Size(max = 1000, message = "A descrição pode ter no máximo 1000 caracteres.")
        String description,

        Long accountId,

        AgendaEventType type,

        LocalDateTime eventDateTime,

        RecurrenceFrequency recurrenceFrequency,

        List<DayOfWeek> daysOfWeek,

        LocalTime timeOfDay,

        Boolean active,

        // Alternativa a recurrenceFrequency/daysOfWeek pra HABIT -- preenchido,
        // tem prioridade (ver DayTypeService). Vazia/nula: comportamento antigo.
        List<DayType> dayTypeTags
) {
}
