package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.dto.request.DayTypeOverrideRequestDTO;
import com.carlos.finhawk_refac.dto.response.DayTypeResponseDTO;
import com.carlos.finhawk_refac.entity.Account;
import com.carlos.finhawk_refac.entity.AgendaEvent;
import com.carlos.finhawk_refac.entity.DayTypeOverride;
import com.carlos.finhawk_refac.entity.UserAccount;
import com.carlos.finhawk_refac.enums.DayType;
import com.carlos.finhawk_refac.enums.RecurrenceFrequency;
import com.carlos.finhawk_refac.repository.AccountRepository;
import com.carlos.finhawk_refac.repository.DayTypeOverrideRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

// Cobre o ciclo fixo de plantao/folga (15/08/2026 = PLANTAO, alternando pra
// sempre a partir dai), a classificacao entrega/fim-de-semana, e o override
// manual (Parte 2) que tem prioridade sobre o calculo automatico -- a base
// de que toda a Agenda 2.0 depende, entao vale conferir datas conhecidas.
@ExtendWith(MockitoExtension.class)
class DayTypeServiceTest {

    private static final Long ACCOUNT_ID = 10L;

    @Mock
    private DayTypeOverrideRepository dayTypeOverrideRepository;
    @Mock
    private AccountRepository accountRepository;

    private DayTypeService service;
    private UserAccount currentUser;
    private Account account;

    @BeforeEach
    void setUp() {
        service = new DayTypeService(dayTypeOverrideRepository, accountRepository);

        currentUser = new UserAccount();
        currentUser.setId(1L);
        currentUser.setEmail("teste@finhawk.app");

        account = new Account();
        account.setId(ACCOUNT_ID);
        account.setUserAccount(currentUser);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null, currentUser.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void stubNoOverride(LocalDate date) {
        when(dayTypeOverrideRepository.findByAccount_IdAndDate(ACCOUNT_ID, date)).thenReturn(Optional.empty());
    }

    @Test
    void anchorDate_ehPlantao() {
        LocalDate date = LocalDate.of(2026, 8, 15);
        stubNoOverride(date);
        assertThat(service.calculate(ACCOUNT_ID, date)).contains(DayType.PLANTAO);
    }

    @Test
    void diaSeguinteAoAnchor_ehFolga() {
        LocalDate date = LocalDate.of(2026, 8, 16);
        stubNoOverride(date);
        assertThat(service.calculate(ACCOUNT_ID, date)).contains(DayType.FOLGA);
    }

    @Test
    void doisDiasAposAnchor_voltaAhSerPlantao() {
        LocalDate date = LocalDate.of(2026, 8, 17);
        stubNoOverride(date);
        assertThat(service.calculate(ACCOUNT_ID, date)).contains(DayType.PLANTAO);
    }

    @Test
    void dataAnteriorAoAnchor_calculaParidadeCorretamente() {
        LocalDate d14 = LocalDate.of(2026, 8, 14);
        LocalDate d13 = LocalDate.of(2026, 8, 13);
        stubNoOverride(d14);
        stubNoOverride(d13);
        // 14/08/2026 esta 1 dia antes do anchor -- distancia impar -> FOLGA.
        assertThat(service.calculate(ACCOUNT_ID, d14)).contains(DayType.FOLGA);
        // 13/08/2026 esta 2 dias antes -- distancia par -> PLANTAO.
        assertThat(service.calculate(ACCOUNT_ID, d13)).contains(DayType.PLANTAO);
    }

    @Test
    void segundaFeiraQualquer_ehEntrega() {
        LocalDate segunda = LocalDate.of(2026, 8, 24);
        stubNoOverride(segunda);
        assertThat(segunda.getDayOfWeek().toString()).isEqualTo("MONDAY");
        assertThat(service.calculate(ACCOUNT_ID, segunda)).contains(DayType.ENTREGA);
    }

    @Test
    void sabadoQualquer_ehFimDeSemana() {
        LocalDate sabado = LocalDate.of(2026, 8, 22);
        stubNoOverride(sabado);
        assertThat(sabado.getDayOfWeek().toString()).isEqualTo("SATURDAY");
        assertThat(service.calculate(ACCOUNT_ID, sabado)).contains(DayType.FIM_DE_SEMANA);
    }

    @Test
    void todaData_temExatamenteDuasEtiquetas() {
        LocalDate date = LocalDate.of(2026, 8, 17);
        stubNoOverride(date);
        assertThat(service.calculate(ACCOUNT_ID, date)).hasSize(2);
    }

    // ===== override manual (Parte 2) =====

    @Test
    void comOverride_usaValorManualEmVezDoCalculoAutomatico() {
        // 17/08/2026 seria PLANTAO pelo calculo automatico -- override diz FOLGA.
        LocalDate date = LocalDate.of(2026, 8, 17);
        DayTypeOverride override = new DayTypeOverride();
        override.setDayType(DayType.FOLGA);
        when(dayTypeOverrideRepository.findByAccount_IdAndDate(ACCOUNT_ID, date)).thenReturn(Optional.of(override));

        assertThat(service.calculate(ACCOUNT_ID, date)).contains(DayType.FOLGA);
    }

    @Test
    void override_naoMexeNaDimensaoEntregaFimDeSemana() {
        // Sabado com override de PLANTAO continua FIM_DE_SEMANA (dimensao
        // independente, nunca sobrescrita).
        LocalDate sabado = LocalDate.of(2026, 8, 22);
        DayTypeOverride override = new DayTypeOverride();
        override.setDayType(DayType.PLANTAO);
        when(dayTypeOverrideRepository.findByAccount_IdAndDate(ACCOUNT_ID, sabado)).thenReturn(Optional.of(override));

        assertThat(service.calculate(ACCOUNT_ID, sabado)).contains(DayType.PLANTAO, DayType.FIM_DE_SEMANA);
    }

    @Test
    void setOverride_criaQuandoNaoExiste_eRetornaEfetivoComOverriddenTrue() {
        LocalDate date = LocalDate.of(2026, 8, 17);
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(dayTypeOverrideRepository.findByAccount_IdAndDate(ACCOUNT_ID, date)).thenReturn(Optional.empty());

        DayTypeResponseDTO result = service.setOverride(ACCOUNT_ID, new DayTypeOverrideRequestDTO(date, DayType.FOLGA));

        assertThat(result.overridden()).isTrue();
        assertThat(result.dayTypes()).contains(DayType.FOLGA);
        org.mockito.Mockito.verify(dayTypeOverrideRepository).save(any(DayTypeOverride.class));
    }

    @Test
    void setOverride_atualizaQuandoJaExiste_naoCriaDuplicata() {
        LocalDate date = LocalDate.of(2026, 8, 17);
        DayTypeOverride existing = new DayTypeOverride();
        existing.setId(99L);
        existing.setDayType(DayType.PLANTAO);

        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(dayTypeOverrideRepository.findByAccount_IdAndDate(ACCOUNT_ID, date)).thenReturn(Optional.of(existing));

        service.setOverride(ACCOUNT_ID, new DayTypeOverrideRequestDTO(date, DayType.FOLGA));

        assertThat(existing.getDayType()).isEqualTo(DayType.FOLGA);
        org.mockito.Mockito.verify(dayTypeOverrideRepository).save(existing);
    }

    @Test
    void setOverride_rejeitaEntregaOuFimDeSemana_soPlantaoOuFolga() {
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.setOverride(ACCOUNT_ID,
                new DayTypeOverrideRequestDTO(LocalDate.of(2026, 8, 17), DayType.ENTREGA)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void setOverride_contaDeOutroUsuario_negaAcesso() {
        UserAccount outroUsuario = new UserAccount();
        outroUsuario.setId(2L);
        Account contaDeOutraPessoa = new Account();
        contaDeOutraPessoa.setId(ACCOUNT_ID);
        contaDeOutraPessoa.setUserAccount(outroUsuario);

        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(contaDeOutraPessoa));

        assertThatThrownBy(() -> service.setOverride(ACCOUNT_ID,
                new DayTypeOverrideRequestDTO(LocalDate.of(2026, 8, 17), DayType.FOLGA)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void getEffectiveDayType_semOverride_retornaOverriddenFalse() {
        LocalDate date = LocalDate.of(2026, 8, 17);
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        stubNoOverride(date);

        DayTypeResponseDTO result = service.getEffectiveDayType(ACCOUNT_ID, date);

        assertThat(result.overridden()).isFalse();
        assertThat(result.dayTypes()).contains(DayType.PLANTAO);
    }

    // ===== habitOccursOn (etiqueta de tipo de dia) =====

    private AgendaEvent habitWithDayType(DayType... tags) {
        AgendaEvent habit = new AgendaEvent();
        habit.setDayTypeTags(List.of(tags));
        habit.setAccount(account);
        return habit;
    }

    @Test
    void habitoComEtiquetaFolga_ocorreSoNosDiasDeFolga() {
        AgendaEvent habit = habitWithDayType(DayType.FOLGA);
        LocalDate d16 = LocalDate.of(2026, 8, 16);
        LocalDate d15 = LocalDate.of(2026, 8, 15);
        LocalDate d17 = LocalDate.of(2026, 8, 17);
        stubNoOverride(d16);
        stubNoOverride(d15);
        stubNoOverride(d17);

        // 16/08/2026 = FOLGA (1 dia apos o anchor).
        assertThat(service.habitOccursOn(habit, d16)).isTrue();
        // 15/08/2026 (anchor) e 17/08/2026 = PLANTAO, nao FOLGA.
        assertThat(service.habitOccursOn(habit, d15)).isFalse();
        assertThat(service.habitOccursOn(habit, d17)).isFalse();
    }

    @Test
    void habitoComEtiquetaFolga_respeitaOverrideManual() {
        // 17/08/2026 seria PLANTAO automatico -- override vira FOLGA, e o
        // habito com etiqueta FOLGA passa a ocorrer nesse dia.
        AgendaEvent habit = habitWithDayType(DayType.FOLGA);
        LocalDate date = LocalDate.of(2026, 8, 17);
        DayTypeOverride override = new DayTypeOverride();
        override.setDayType(DayType.FOLGA);
        when(dayTypeOverrideRepository.findByAccount_IdAndDate(ACCOUNT_ID, date)).thenReturn(Optional.of(override));

        assertThat(service.habitOccursOn(habit, date)).isTrue();
    }

    @Test
    void habitoComVariasEtiquetas_ocorreSeQualquerUmaBater_logicaOU() {
        // Sabado 22/08/2026 e PLANTAO (distancia par) + FIM_DE_SEMANA -- so
        // uma das duas etiquetas do habito precisa bater.
        AgendaEvent habit = habitWithDayType(DayType.FOLGA, DayType.FIM_DE_SEMANA);
        LocalDate sabado = LocalDate.of(2026, 8, 22);
        stubNoOverride(sabado);
        assertThat(service.habitOccursOn(habit, sabado)).isTrue();
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
