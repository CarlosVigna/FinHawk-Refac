package com.carlos.finhawk_refac.scheduler;

import com.carlos.finhawk_refac.entity.AgendaEvent;
import com.carlos.finhawk_refac.entity.AgendaEventCompletion;
import com.carlos.finhawk_refac.entity.Bill;
import com.carlos.finhawk_refac.entity.WeeklyGoal;
import com.carlos.finhawk_refac.enums.AgendaCompletionStatus;
import com.carlos.finhawk_refac.enums.AgendaEventType;
import com.carlos.finhawk_refac.enums.StatusBill;
import com.carlos.finhawk_refac.repository.AgendaEventCompletionRepository;
import com.carlos.finhawk_refac.repository.AgendaEventRepository;
import com.carlos.finhawk_refac.repository.BillRepository;
import com.carlos.finhawk_refac.repository.NotificationLogRepository;
import com.carlos.finhawk_refac.repository.WeeklyGoalRepository;
import com.carlos.finhawk_refac.service.DayTypeService;
import com.carlos.finhawk_refac.service.WhatsAppNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Cobre os resumos de hoje/semana (Parte 3): a separacao em "ainda falta" /
// "ja feito", e que uma secao inteira some da mensagem quando fica vazia
// (nao aparece cabecalho sem conteudo embaixo).
@ExtendWith(MockitoExtension.class)
class AgendaNotificationSchedulerTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    @Mock private AgendaEventRepository agendaEventRepository;
    @Mock private BillRepository billRepository;
    @Mock private AgendaEventCompletionRepository agendaEventCompletionRepository;
    @Mock private WeeklyGoalRepository weeklyGoalRepository;
    @Mock private NotificationLogRepository notificationLogRepository;
    @Mock private WhatsAppNotificationService whatsAppNotificationService;
    @Mock private DayTypeService dayTypeService;

    private AgendaNotificationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new AgendaNotificationScheduler(agendaEventRepository, billRepository,
                agendaEventCompletionRepository, weeklyGoalRepository, notificationLogRepository,
                whatsAppNotificationService, dayTypeService);
    }

    private static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    private AgendaEvent oneTimeEvent(Long id, String title, LocalDate date, LocalTime time) {
        AgendaEvent e = new AgendaEvent();
        e.setId(id);
        e.setTitle(title);
        e.setType(AgendaEventType.ONE_TIME);
        e.setEventDateTime(date.atTime(time));
        e.setActive(true);
        return e;
    }

    private AgendaEvent habit(Long id, String title, LocalTime time) {
        AgendaEvent h = new AgendaEvent();
        h.setId(id);
        h.setTitle(title);
        h.setType(AgendaEventType.HABIT);
        h.setTimeOfDay(time);
        h.setActive(true);
        return h;
    }

    private Bill bill(Long id, String description, LocalDate maturity, StatusBill status) {
        Bill b = new Bill();
        b.setId(id);
        b.setDescription(description);
        b.setMaturity(maturity);
        b.setStatus(status);
        b.setInstallmentAmount(BigDecimal.valueOf(100));
        return b;
    }

    private WeeklyGoal goal(Long id, String title, boolean completed) {
        WeeklyGoal g = new WeeklyGoal();
        g.setId(id);
        g.setTitle(title);
        g.setCompleted(completed);
        return g;
    }

    private AgendaEventCompletion doneCompletion(AgendaEvent event) {
        AgendaEventCompletion c = new AgendaEventCompletion();
        c.setAgendaEvent(event);
        c.setStatus(AgendaCompletionStatus.DONE);
        return c;
    }

    private String captureSentMessage() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(whatsAppNotificationService).sendMessage(captor.capture());
        return captor.getValue();
    }

    // ===== notifyToday =====

    @Test
    void notifyToday_separaFeitoDePendente() {
        LocalDate today = today();
        AgendaEvent pendingEvent = oneTimeEvent(1L, "Consulta médica", today, LocalTime.of(14, 30));
        AgendaEvent doneHabit = habit(2L, "Tomar remédio", LocalTime.of(8, 0));
        Bill pendingBill = bill(3L, "Luz", today, StatusBill.PENDING);
        WeeklyGoal doneGoal = goal(4L, "Treinar 3x", true);

        when(agendaEventRepository.findAllByTypeAndActiveTrueAndDeletedAtIsNullAndEventDateTimeBetween(
                AgendaEventType.ONE_TIME, today.atStartOfDay(), today.plusDays(1).atStartOfDay()))
                .thenReturn(List.of(pendingEvent));
        when(agendaEventRepository.findAllByTypeAndActiveTrueAndDeletedAtIsNull(AgendaEventType.HABIT))
                .thenReturn(List.of(doneHabit));
        when(dayTypeService.habitOccursOn(any(), any())).thenReturn(true);
        when(billRepository.findAllByMaturity(today)).thenReturn(List.of(pendingBill));

        LocalDate weekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        when(weeklyGoalRepository.findAllByWeekStartDate(weekMonday)).thenReturn(List.of(doneGoal));

        when(agendaEventCompletionRepository.findAllByEventDate(today))
                .thenReturn(List.of(doneCompletion(doneHabit)));

        int count = scheduler.notifyToday();

        assertThat(count).isEqualTo(4);
        String message = captureSentMessage();
        assertThat(message).contains("⏳ Ainda falta:");
        assertThat(message).contains("✅ Já feito:");

        int pendingIdx = message.indexOf("⏳ Ainda falta:");
        int doneIdx = message.indexOf("✅ Já feito:");
        int eventIdx = message.indexOf("Consulta médica");
        int billIdx = message.indexOf("Luz");
        int habitIdx = message.indexOf("Tomar remédio");
        int goalIdx = message.indexOf("Treinar 3x");

        // evento e conta pendentes ficam na secao "ainda falta"
        assertThat(eventIdx).isBetween(pendingIdx, doneIdx);
        assertThat(billIdx).isBetween(pendingIdx, doneIdx);
        // habito e meta ja concluidos ficam na secao "ja feito"
        assertThat(habitIdx).isGreaterThan(doneIdx);
        assertThat(goalIdx).isGreaterThan(doneIdx);
    }

    @Test
    void notifyToday_tudoFeito_omiteSecaoAindaFalta() {
        LocalDate today = today();
        AgendaEvent doneEvent = oneTimeEvent(1L, "Consulta médica", today, LocalTime.of(14, 30));

        when(agendaEventRepository.findAllByTypeAndActiveTrueAndDeletedAtIsNullAndEventDateTimeBetween(
                AgendaEventType.ONE_TIME, today.atStartOfDay(), today.plusDays(1).atStartOfDay()))
                .thenReturn(List.of(doneEvent));
        when(agendaEventRepository.findAllByTypeAndActiveTrueAndDeletedAtIsNull(AgendaEventType.HABIT))
                .thenReturn(List.of());
        when(billRepository.findAllByMaturity(today)).thenReturn(List.of());

        LocalDate weekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        when(weeklyGoalRepository.findAllByWeekStartDate(weekMonday)).thenReturn(List.of());

        when(agendaEventCompletionRepository.findAllByEventDate(today))
                .thenReturn(List.of(doneCompletion(doneEvent)));

        scheduler.notifyToday();

        String message = captureSentMessage();
        assertThat(message).doesNotContain("⏳ Ainda falta:");
        assertThat(message).contains("✅ Já feito:");
        assertThat(message).contains("Consulta médica");
    }

    @Test
    void notifyToday_semNadaHoje_mandaMensagemDeVazio_eRetornaZero() {
        LocalDate today = today();

        when(agendaEventRepository.findAllByTypeAndActiveTrueAndDeletedAtIsNullAndEventDateTimeBetween(
                AgendaEventType.ONE_TIME, today.atStartOfDay(), today.plusDays(1).atStartOfDay()))
                .thenReturn(List.of());
        when(agendaEventRepository.findAllByTypeAndActiveTrueAndDeletedAtIsNull(AgendaEventType.HABIT))
                .thenReturn(List.of());
        when(billRepository.findAllByMaturity(today)).thenReturn(List.of());

        LocalDate weekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        when(weeklyGoalRepository.findAllByWeekStartDate(weekMonday)).thenReturn(List.of());

        int count = scheduler.notifyToday();

        assertThat(count).isEqualTo(0);
        String message = captureSentMessage();
        assertThat(message).contains("nada agendado");
    }

    // ===== notifyWeek =====

    @Test
    void notifyWeek_separaFeitoDePendente() {
        LocalDate today = today();
        LocalDate start = today.plusDays(1);
        LocalDate end = today.plusDays(7);
        LocalDate inWeek = today.plusDays(3);

        AgendaEvent pendingEvent = oneTimeEvent(1L, "Reunião", inWeek, LocalTime.of(10, 0));
        Bill doneBill = bill(2L, "Internet", inWeek, StatusBill.PAID);

        when(agendaEventRepository.findAllByTypeAndActiveTrueAndDeletedAtIsNullAndEventDateTimeBetween(
                AgendaEventType.ONE_TIME, start.atStartOfDay(), end.plusDays(1).atStartOfDay()))
                .thenReturn(List.of(pendingEvent));
        when(billRepository.findAllByMaturityBetween(start, end)).thenReturn(List.of(doneBill));

        LocalDate weekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        when(weeklyGoalRepository.findAllByWeekStartDate(weekMonday)).thenReturn(List.of());

        when(agendaEventCompletionRepository.findAllByEventDateBetween(start, end)).thenReturn(List.of());

        int count = scheduler.notifyWeek();

        assertThat(count).isEqualTo(2);
        String message = captureSentMessage();
        int pendingIdx = message.indexOf("⏳ Ainda falta:");
        int doneIdx = message.indexOf("✅ Já feito:");

        assertThat(message.indexOf("Reunião")).isBetween(pendingIdx, doneIdx);
        assertThat(message.indexOf("Internet")).isGreaterThan(doneIdx);
    }

    @Test
    void notifyWeek_semNada_mandaMensagemDeVazio() {
        LocalDate today = today();
        LocalDate start = today.plusDays(1);
        LocalDate end = today.plusDays(7);

        when(agendaEventRepository.findAllByTypeAndActiveTrueAndDeletedAtIsNullAndEventDateTimeBetween(
                AgendaEventType.ONE_TIME, start.atStartOfDay(), end.plusDays(1).atStartOfDay()))
                .thenReturn(List.of());
        when(billRepository.findAllByMaturityBetween(start, end)).thenReturn(List.of());

        LocalDate weekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        when(weeklyGoalRepository.findAllByWeekStartDate(weekMonday)).thenReturn(List.of());

        int count = scheduler.notifyWeek();

        assertThat(count).isEqualTo(0);
        String message = captureSentMessage();
        assertThat(message).contains("nada agendado");
    }
}
