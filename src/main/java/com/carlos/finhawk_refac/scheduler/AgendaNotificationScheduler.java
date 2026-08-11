package com.carlos.finhawk_refac.scheduler;

import com.carlos.finhawk_refac.entity.AgendaEvent;
import com.carlos.finhawk_refac.entity.Bill;
import com.carlos.finhawk_refac.entity.NotificationLog;
import com.carlos.finhawk_refac.enums.AgendaEventType;
import com.carlos.finhawk_refac.enums.CategoryType;
import com.carlos.finhawk_refac.enums.RecurrenceFrequency;
import com.carlos.finhawk_refac.enums.StatusBill;
import com.carlos.finhawk_refac.repository.AgendaEventRepository;
import com.carlos.finhawk_refac.repository.BillRepository;
import com.carlos.finhawk_refac.repository.NotificationLogRepository;
import com.carlos.finhawk_refac.service.WhatsAppNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

// Schedulers da agenda/vencimentos -- todos no fuso America/Sao_Paulo.
// Cada job so envia notificacao se houver conteudo, e cada job de
// periodicidade fixa (noturno/semanal/matinal) registra em
// NotificationLog pra nao reenviar a mesma notificacao caso o scheduler
// dispare de novo pra mesma data (ex: restart do servico). O lembrete
// "1h antes" usa o campo reminderSentAt do proprio AgendaEvent pro mesmo fim.
// Excecoes sao sempre capturadas e logadas aqui -- falha de notificacao
// nunca pode derrubar o restante do sistema.
@Component
public class AgendaNotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(AgendaNotificationScheduler.class);
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    // Usado nas notificacoes de ciclo de vida (atrasada/fechamento), que
    // podem se referir a qualquer mes/ano -- diferente do DATE_FMT acima,
    // que so aparece em avisos de curtissimo prazo ("vence amanha").
    private static final DateTimeFormatter DATE_FMT_FULL = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MM/yyyy");

    private static final String JOB_NIGHTLY_SUMMARY = "NIGHTLY_SUMMARY";
    private static final String JOB_WEEKLY_SUMMARY = "WEEKLY_SUMMARY";
    private static final String JOB_MORNING_DUE_TODAY = "MORNING_DUE_TODAY";
    private static final String JOB_OVERDUE_BILLS = "OVERDUE_BILLS";
    private static final String JOB_MONTHLY_CLOSING = "MONTHLY_CLOSING";

    private final AgendaEventRepository agendaEventRepository;
    private final BillRepository billRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final WhatsAppNotificationService whatsAppNotificationService;

    public AgendaNotificationScheduler(AgendaEventRepository agendaEventRepository,
                                        BillRepository billRepository,
                                        NotificationLogRepository notificationLogRepository,
                                        WhatsAppNotificationService whatsAppNotificationService) {
        this.agendaEventRepository = agendaEventRepository;
        this.billRepository = billRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.whatsAppNotificationService = whatsAppNotificationService;
    }

    private static String formatCurrency(BigDecimal value) {
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return fmt.format(value != null ? value : BigDecimal.ZERO);
    }

    private boolean habitOccursOn(AgendaEvent habit, LocalDate date) {
        if (habit.getRecurrenceFrequency() == RecurrenceFrequency.DAILY) {
            return true;
        }
        if (habit.getRecurrenceFrequency() == RecurrenceFrequency.WEEKLY) {
            return habit.getDaysOfWeek() != null && habit.getDaysOfWeek().contains(date.getDayOfWeek());
        }
        return false;
    }

    // ===== Resumo noturno diario (21h): agenda de amanha + vencimentos de amanha =====

    @Scheduled(cron = "0 0 21 * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void nightlySummary() {
        try {
            LocalDate today = LocalDate.now(ZONE);
            LocalDate tomorrow = today.plusDays(1);

            if (notificationLogRepository.existsByJobKeyAndReferenceDate(JOB_NIGHTLY_SUMMARY, today)) {
                return;
            }

            List<AgendaEvent> oneTimeTomorrow = agendaEventRepository
                    .findAllByTypeAndActiveTrueAndDeletedAtIsNullAndEventDateTimeBetween(
                            AgendaEventType.ONE_TIME, tomorrow.atStartOfDay(), tomorrow.plusDays(1).atStartOfDay());

            List<AgendaEvent> habitsTomorrow = agendaEventRepository
                    .findAllByTypeAndActiveTrueAndDeletedAtIsNull(AgendaEventType.HABIT)
                    .stream()
                    .filter(h -> habitOccursOn(h, tomorrow))
                    .toList();

            List<Bill> billsTomorrow = billRepository.findAllByMaturityAndStatus(tomorrow, StatusBill.PENDING);

            if (!oneTimeTomorrow.isEmpty() || !habitsTomorrow.isEmpty() || !billsTomorrow.isEmpty()) {
                StringBuilder sb = new StringBuilder("📋 Resumo de amanhã (" + tomorrow.format(DATE_FMT) + "):\n");

                oneTimeTomorrow.stream()
                        .sorted((a, b) -> a.getEventDateTime().compareTo(b.getEventDateTime()))
                        .forEach(e -> sb.append("\n📅 ").append(e.getTitle())
                                .append(" às ").append(e.getEventDateTime().format(TIME_FMT)));

                habitsTomorrow.stream()
                        .sorted((a, b) -> a.getTimeOfDay().compareTo(b.getTimeOfDay()))
                        .forEach(h -> sb.append("\n🔁 ").append(h.getTitle())
                                .append(" às ").append(h.getTimeOfDay().format(TIME_FMT)));

                billsTomorrow.forEach(b -> sb.append("\n💰 Vence amanhã: ").append(b.getDescription())
                        .append(" — ").append(formatCurrency(b.getInstallmentAmount())));

                whatsAppNotificationService.sendMessage(sb.toString());
            }

            markSent(JOB_NIGHTLY_SUMMARY, today);
        } catch (Exception e) {
            log.error("Falha no job de resumo noturno da agenda: {}", e.getMessage(), e);
        }
    }

    // ===== Lembrete "1h antes" (a cada 5 minutos, janela de 55-65min) =====

    @Scheduled(cron = "0 */5 * * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void oneHourReminder() {
        try {
            LocalDateTime now = LocalDateTime.now(ZONE);
            LocalDateTime windowStart = now.plusMinutes(55);
            LocalDateTime windowEnd = now.plusMinutes(65);

            List<AgendaEvent> due = agendaEventRepository
                    .findAllByTypeAndActiveTrueAndDeletedAtIsNullAndReminderSentAtIsNullAndEventDateTimeBetween(
                            AgendaEventType.ONE_TIME, windowStart, windowEnd);

            if (due.isEmpty()) {
                return;
            }

            StringBuilder sb = new StringBuilder("⏰ Daqui a ~1h:\n");
            due.stream()
                    .sorted((a, b) -> a.getEventDateTime().compareTo(b.getEventDateTime()))
                    .forEach(e -> sb.append("\n📌 ").append(e.getTitle())
                            .append(" às ").append(e.getEventDateTime().format(TIME_FMT)));

            whatsAppNotificationService.sendMessage(sb.toString());

            LocalDateTime sentAt = LocalDateTime.now(ZONE);
            due.forEach(e -> e.setReminderSentAt(sentAt));
            agendaEventRepository.saveAll(due);
        } catch (Exception e) {
            log.error("Falha no job de lembrete '1h antes' da agenda: {}", e.getMessage(), e);
        }
    }

    // ===== Resumo semanal (domingo, 21h): vencimentos dos proximos 7 dias =====

    @Scheduled(cron = "0 0 21 * * SUN", zone = "America/Sao_Paulo")
    @Transactional
    public void weeklySummary() {
        try {
            LocalDate today = LocalDate.now(ZONE);

            if (notificationLogRepository.existsByJobKeyAndReferenceDate(JOB_WEEKLY_SUMMARY, today)) {
                return;
            }

            LocalDate start = today.plusDays(1);
            LocalDate end = today.plusDays(7);

            List<Bill> bills = billRepository.findAllByMaturityBetweenAndStatus(start, end, StatusBill.PENDING);

            if (!bills.isEmpty()) {
                StringBuilder sb = new StringBuilder("📆 Resumo da semana — vencimentos dos próximos 7 dias:\n");
                bills.stream()
                        .sorted((a, b) -> a.getMaturity().compareTo(b.getMaturity()))
                        .forEach(b -> sb.append("\n💰 ").append(b.getMaturity().format(DATE_FMT))
                                .append(" — ").append(b.getDescription())
                                .append(" — ").append(formatCurrency(b.getInstallmentAmount())));

                whatsAppNotificationService.sendMessage(sb.toString());
            }

            markSent(JOB_WEEKLY_SUMMARY, today);
        } catch (Exception e) {
            log.error("Falha no job de resumo semanal de vencimentos: {}", e.getMessage(), e);
        }
    }

    // ===== Aviso matinal de vencimento hoje (7h) =====

    @Scheduled(cron = "0 0 7 * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void morningDueToday() {
        try {
            LocalDate today = LocalDate.now(ZONE);

            if (notificationLogRepository.existsByJobKeyAndReferenceDate(JOB_MORNING_DUE_TODAY, today)) {
                return;
            }

            List<Bill> bills = billRepository.findAllByMaturityAndStatus(today, StatusBill.PENDING);

            if (!bills.isEmpty()) {
                StringBuilder sb = new StringBuilder("☀️ Bom dia! Vence hoje:\n");
                bills.forEach(b -> sb.append("\n💰 ").append(b.getDescription())
                        .append(" — ").append(formatCurrency(b.getInstallmentAmount())));

                whatsAppNotificationService.sendMessage(sb.toString());
            }

            markSent(JOB_MORNING_DUE_TODAY, today);
        } catch (Exception e) {
            log.error("Falha no job de aviso matinal de vencimento: {}", e.getMessage(), e);
        }
    }

    // ===== Contas recem-atrasadas (8h): venceram ontem e continuam PENDING =====

    @Scheduled(cron = "0 0 8 * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void overdueBills() {
        try {
            LocalDate today = LocalDate.now(ZONE);

            if (notificationLogRepository.existsByJobKeyAndReferenceDate(JOB_OVERDUE_BILLS, today)) {
                return;
            }

            LocalDate yesterday = today.minusDays(1);
            List<Bill> bills = billRepository.findAllByMaturityAndStatus(yesterday, StatusBill.PENDING);

            if (!bills.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                bills.forEach(b -> {
                    if (sb.length() > 0) {
                        sb.append("\n");
                    }
                    sb.append("🚨 Atrasada: ").append(b.getDescription())
                            .append(" — ").append(formatCurrency(b.getInstallmentAmount()))
                            .append(" (venceu ").append(b.getMaturity().format(DATE_FMT_FULL)).append(").");
                });

                whatsAppNotificationService.sendMessage(sb.toString());
            }

            markSent(JOB_OVERDUE_BILLS, today);
        } catch (Exception e) {
            log.error("Falha no job de contas recem-atrasadas: {}", e.getMessage(), e);
        }
    }

    // ===== Fechamento mensal (dia 1, 9h): resumo do mes anterior =====

    @Scheduled(cron = "0 0 9 1 * *", zone = "America/Sao_Paulo")
    @Transactional
    public void monthlyClosing() {
        try {
            LocalDate today = LocalDate.now(ZONE);

            if (notificationLogRepository.existsByJobKeyAndReferenceDate(JOB_MONTHLY_CLOSING, today)) {
                return;
            }

            LocalDate lastMonth = today.minusMonths(1);
            LocalDate start = lastMonth.withDayOfMonth(1);
            LocalDate end = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth());

            BigDecimal entradas = billRepository.findAllByMaturityBetweenAndStatus(start, end, StatusBill.RECEIVED)
                    .stream()
                    .filter(b -> b.getCategory().getType() == CategoryType.RECEIPT)
                    .map(b -> b.getInstallmentAmount() != null ? b.getInstallmentAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal saidas = billRepository.findAllByMaturityBetweenAndStatus(start, end, StatusBill.PAID)
                    .stream()
                    .filter(b -> b.getCategory().getType() == CategoryType.PAYMENT)
                    .map(b -> b.getInstallmentAmount() != null ? b.getInstallmentAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal saldo = entradas.subtract(saidas);

            whatsAppNotificationService.sendMessage(
                    "📊 Fechamento de " + start.format(MONTH_FMT) + ": Entradas " + formatCurrency(entradas)
                            + ", Saídas " + formatCurrency(saidas) + ", Saldo " + formatCurrency(saldo) + ".");

            markSent(JOB_MONTHLY_CLOSING, today);
        } catch (Exception e) {
            log.error("Falha no job de fechamento mensal: {}", e.getMessage(), e);
        }
    }

    private void markSent(String jobKey, LocalDate referenceDate) {
        NotificationLog entry = new NotificationLog();
        entry.setJobKey(jobKey);
        entry.setReferenceDate(referenceDate);
        entry.setSentAt(LocalDateTime.now(ZONE));
        notificationLogRepository.save(entry);
    }
}
