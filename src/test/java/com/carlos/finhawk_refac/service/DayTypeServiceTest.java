package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.enums.DayType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

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
}
