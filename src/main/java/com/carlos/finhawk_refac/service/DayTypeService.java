package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.enums.DayType;
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
}
