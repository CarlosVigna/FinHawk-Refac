package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.entity.AgendaEvent;
import com.carlos.finhawk_refac.enums.DayType;
import com.carlos.finhawk_refac.enums.RecurrenceFrequency;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Set;

// Calcula os tipos de dia (plantao/folga/entrega/fim de semana) aplicaveis a
// uma data.
//
// O ciclo de plantao/folga e fixo pra sempre, combinado com o usuario:
// 15/08/2026 = PLANTAO, alternando dia a dia a partir dai. De proposito uma
// constante fixa no codigo (nao variavel de ambiente/editavel pelo usuario)
// nesta versao -- mudar isso exigiria trocar o codigo e fazer deploy.
@Service
public class DayTypeService {

    private static final LocalDate PLANTAO_ANCHOR = LocalDate.of(2026, 8, 15);

    // Toda data tem exatamente um de {PLANTAO, FOLGA} + exatamente um de
    // {ENTREGA, FIM_DE_SEMANA} -- sempre 2 etiquetas.
    public Set<DayType> calculate(LocalDate date) {
        Set<DayType> tags = EnumSet.noneOf(DayType.class);

        long diff = ChronoUnit.DAYS.between(PLANTAO_ANCHOR, date);
        // Math.floorMod (nao o operador %) pra tratar corretamente datas
        // anteriores ao anchor, onde diff e negativo.
        boolean isPlantao = Math.floorMod(diff, 2) == 0;
        tags.add(isPlantao ? DayType.PLANTAO : DayType.FOLGA);

        DayOfWeek dow = date.getDayOfWeek();
        boolean isWeekend = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
        tags.add(isWeekend ? DayType.FIM_DE_SEMANA : DayType.ENTREGA);

        return tags;
    }

    // "Esse habito ocorre nessa data?" -- se o habito tiver etiquetas de tipo
    // de dia configuradas, elas tem prioridade (logica OU: basta uma bater
    // com o tipo do dia) e a frequencia antiga (DAILY/WEEKLY) e ignorada. Se
    // nao tiver etiqueta nenhuma, usa a logica antiga como sempre foi.
    public boolean habitOccursOn(AgendaEvent habit, LocalDate date) {
        if (habit.getDayTypeTags() != null && !habit.getDayTypeTags().isEmpty()) {
            Set<DayType> todayTags = calculate(date);
            return habit.getDayTypeTags().stream().anyMatch(todayTags::contains);
        }

        if (habit.getRecurrenceFrequency() == RecurrenceFrequency.DAILY) {
            return true;
        }
        if (habit.getRecurrenceFrequency() == RecurrenceFrequency.WEEKLY) {
            return habit.getDaysOfWeek() != null && habit.getDaysOfWeek().contains(date.getDayOfWeek());
        }
        return false;
    }
}
