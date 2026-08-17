package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.entity.AgendaEvent;
import com.carlos.finhawk_refac.enums.DayType;
import com.carlos.finhawk_refac.enums.RecurrenceFrequency;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Cobre o ciclo fixo de plantao/folga (15/08/2026 = PLANTAO, alternando pra
// sempre a partir dai) e a classificacao entrega/fim-de-semana -- a base de
// que toda a Agenda 2.0 depende, entao vale conferir datas conhecidas.
class DayTypeServiceTest {

    private final DayTypeService service = new DayTypeService();

    @Test
    void anchorDate_ehPlantao() {
        assertThat(service.calculate(LocalDate.of(2026, 8, 15))).contains(DayType.PLANTAO);
    }

    @Test
    void diaSeguinteAoAnchor_ehFolga() {
        assertThat(service.calculate(LocalDate.of(2026, 8, 16))).contains(DayType.FOLGA);
    }

    @Test
    void doisDiasAposAnchor_voltaAhSerPlantao() {
        assertThat(service.calculate(LocalDate.of(2026, 8, 17))).contains(DayType.PLANTAO);
    }

    @Test
    void dataAnteriorAoAnchor_calculaParidadeCorretamente() {
        // 14/08/2026 esta 1 dia antes do anchor -- distancia impar -> FOLGA.
        assertThat(service.calculate(LocalDate.of(2026, 8, 14))).contains(DayType.FOLGA);
        // 13/08/2026 esta 2 dias antes -- distancia par -> PLANTAO.
        assertThat(service.calculate(LocalDate.of(2026, 8, 13))).contains(DayType.PLANTAO);
    }

    @Test
    void segundaFeiraQualquer_ehEntrega() {
        LocalDate segunda = LocalDate.of(2026, 8, 24);
        assertThat(segunda.getDayOfWeek().toString()).isEqualTo("MONDAY");
        assertThat(service.calculate(segunda)).contains(DayType.ENTREGA);
    }

    @Test
    void sabadoQualquer_ehFimDeSemana() {
        LocalDate sabado = LocalDate.of(2026, 8, 22);
        assertThat(sabado.getDayOfWeek().toString()).isEqualTo("SATURDAY");
        assertThat(service.calculate(sabado)).contains(DayType.FIM_DE_SEMANA);
    }

    @Test
    void todaData_temExatamenteDuasEtiquetas() {
        assertThat(service.calculate(LocalDate.of(2026, 8, 17))).hasSize(2);
    }

    // ===== habitOccursOn (Parte 2 -- etiqueta de tipo de dia) =====

    private AgendaEvent habitWithDayType(DayType... tags) {
        AgendaEvent habit = new AgendaEvent();
        habit.setDayTypeTags(List.of(tags));
        return habit;
    }

    @Test
    void habitoComEtiquetaFolga_ocorreSoNosDiasDeFolga() {
        AgendaEvent habit = habitWithDayType(DayType.FOLGA);

        // 16/08/2026 = FOLGA (1 dia apos o anchor).
        assertThat(service.habitOccursOn(habit, LocalDate.of(2026, 8, 16))).isTrue();
        // 15/08/2026 (anchor) e 17/08/2026 = PLANTAO, nao FOLGA.
        assertThat(service.habitOccursOn(habit, LocalDate.of(2026, 8, 15))).isFalse();
        assertThat(service.habitOccursOn(habit, LocalDate.of(2026, 8, 17))).isFalse();
    }

    @Test
    void habitoComVariasEtiquetas_ocorreSeQualquerUmaBater_logicaOU() {
        // Sabado 22/08/2026 e PLANTAO (distancia par) + FIM_DE_SEMANA -- so
        // uma das duas etiquetas do habito precisa bater.
        AgendaEvent habit = habitWithDayType(DayType.FOLGA, DayType.FIM_DE_SEMANA);
        assertThat(service.habitOccursOn(habit, LocalDate.of(2026, 8, 22))).isTrue();
    }

    @Test
    void habitoSemEtiqueta_usaFrequenciaAntigaDaily() {
        AgendaEvent habit = new AgendaEvent();
        habit.setDayTypeTags(List.of());
        habit.setRecurrenceFrequency(RecurrenceFrequency.DAILY);

        assertThat(service.habitOccursOn(habit, LocalDate.of(2026, 8, 15))).isTrue();
        assertThat(service.habitOccursOn(habit, LocalDate.of(2026, 8, 16))).isTrue();
    }

    @Test
    void habitoSemEtiqueta_usaFrequenciaAntigaWeekly() {
        AgendaEvent habit = new AgendaEvent();
        habit.setDayTypeTags(null);
        habit.setRecurrenceFrequency(RecurrenceFrequency.WEEKLY);
        habit.setDaysOfWeek(List.of(java.time.DayOfWeek.MONDAY));

        LocalDate segunda = LocalDate.of(2026, 8, 24);
        LocalDate terca = LocalDate.of(2026, 8, 25);
        assertThat(service.habitOccursOn(habit, segunda)).isTrue();
        assertThat(service.habitOccursOn(habit, terca)).isFalse();
    }
}
