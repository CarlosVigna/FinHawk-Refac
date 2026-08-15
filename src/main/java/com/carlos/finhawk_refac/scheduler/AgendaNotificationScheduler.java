package com.carlos.finhawk_refac.scheduler;

import com.carlos.finhawk_refac.entity.AgendaEvent;
import com.carlos.finhawk_refac.entity.Bill;
import com.carlos.finhawk_refac.entity.NotificationLog;
import com.carlos.finhawk_refac.enums.AgendaEventType;
import com.carlos.finhawk_refac.enums.CategoryType;
import com.carlos.finhawk_refac.enums.RecurrenceFrequency;
import com.carlos.finhawk_refac.enums.StatusBill;
import com.carlos.finhawk_refac.repository.AgendaEventCompletionRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
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
    private static final String JOB_HABIT_FORGOTTEN_PREFIX = "HABIT_FORGOTTEN_";
    // Limiares (em dias, desde a primeira ocorrencia esquecida) em que o
    // habito esquecido gera aviso -- so nesses dias exatos, pra nao virar
    // spam diario enquanto o habito continua esquecido.
    private static final int[] HABIT_FORGOTTEN_THRESHOLDS = {2, 5};
    private static final int HABIT_FORGOTTEN_MAX_LOOKBACK_DAYS = 30;

    private final AgendaEventRepository agendaEventRepository;
    private final BillRepository billRepository;
    private final AgendaEventCompletionRepository agendaEventCompletionRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final WhatsAppNotificationService whatsAppNotificationService;

    public AgendaNotificationScheduler(AgendaEventRepository agendaEventRepository,
                                        BillRepository billRepository,
                                        AgendaEventCompletionRepository agendaEventCompletionRepository,
                                        NotificationLogRepository notificationLogRepository,
                                        WhatsAppNotificationService whatsAppNotificationService) {
        this.agendaEventRepository = agendaEventRepository;
        this.billRepository = billRepository;
        this.agendaEventCompletionRepository = agendaEventCompletionRepository;
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

    // Eventos pontuais ativos programados pra uma data especifica -- usado
    // pelo resumo noturno (amanha) e pelo resumo sob demanda de hoje.
    private List<AgendaEvent> oneTimeEventsOn(LocalDate date) {
        return agendaEventRepository
                .findAllByTypeAndActiveTrueAndDeletedAtIsNullAndEventDateTimeBetween(
                        AgendaEventType.ONE_TIME, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    // Habitos ativos que ocorrem numa data especifica -- mesmo uso do metodo acima.
    private List<AgendaEvent> habitsOn(LocalDate date) {
        return agendaEventRepository.findAllByTypeAndActiveTrueAndDeletedAtIsNull(AgendaEventType.HABIT)
                .stream()
                .filter(h -> habitOccursOn(h, date))
                .toList();
    }

    // Linhas de evento/habito ("📅 titulo as HH:mm" / "🔁 titulo as HH:mm"),
    // reaproveitadas pelo resumo noturno e pelo resumo sob demanda de hoje.
    private void appendEventAndHabitLines(StringBuilder sb, List<AgendaEvent> oneTimeEvents, List<AgendaEvent> habits) {
        oneTimeEvents.stream()
                .sorted((a, b) -> a.getEventDateTime().compareTo(b.getEventDateTime()))
                .forEach(e -> sb.append("\n📅 ").append(e.getTitle())
                        .append(" às ").append(e.getEventDateTime().format(TIME_FMT)));

        habits.stream()
                .sorted((a, b) -> a.getTimeOfDay().compareTo(b.getTimeOfDay()))
                .forEach(h -> sb.append("\n🔁 ").append(h.getTitle())
                        .append(" às ").append(h.getTimeOfDay().format(TIME_FMT)));
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

            List<AgendaEvent> oneTimeTomorrow = oneTimeEventsOn(tomorrow);
            List<AgendaEvent> habitsTomorrow = habitsOn(tomorrow);
            List<Bill> billsTomorrow = billRepository.findAllByMaturityAndStatus(tomorrow, StatusBill.PENDING);

            if (!oneTimeTomorrow.isEmpty() || !habitsTomorrow.isEmpty() || !billsTomorrow.isEmpty()) {
                StringBuilder sb = new StringBuilder("📋 Resumo de amanhã (" + tomorrow.format(DATE_FMT) + "):\n");

                appendEventAndHabitLines(sb, oneTimeTomorrow, habitsTomorrow);

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

    // ===== Habito esquecido (20:45, antes do resumo noturno): habitos ativos =====
    // ===== sem completion ha 2+ dias, avisando de novo so ao cruzar 5 dias =====

    @Scheduled(cron = "0 45 20 * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void forgottenHabits() {
        try {
            LocalDate today = LocalDate.now(ZONE);

            List<AgendaEvent> habits = agendaEventRepository
                    .findAllByTypeAndActiveTrueAndDeletedAtIsNull(AgendaEventType.HABIT);

            StringBuilder sb = new StringBuilder();

            for (AgendaEvent habit : habits) {
                Integer daysSince = daysSinceForgotten(habit, today);

                if (daysSince == null || Arrays.stream(HABIT_FORGOTTEN_THRESHOLDS)
                        .noneMatch(t -> t == daysSince)) {
                    continue;
                }

                String jobKey = JOB_HABIT_FORGOTTEN_PREFIX + habit.getId() + "_" + daysSince;
                if (notificationLogRepository.existsByJobKeyAndReferenceDate(jobKey, today)) {
                    continue;
                }

                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append("⚠️ Você não marcou '").append(habit.getTitle())
                        .append("' há ").append(daysSince).append(" dia(s).");

                markSent(jobKey, today);
            }

            if (sb.length() > 0) {
                whatsAppNotificationService.sendMessage(sb.toString());
            }
        } catch (Exception e) {
            log.error("Falha no job de habito esquecido: {}", e.getMessage(), e);
        }
    }

    // Calcula ha quantos dias (a partir de hoje) o habito esta esquecido: anda
    // pra tras dia a dia a partir de ontem, e para no primeiro dia em que o
    // habito deveria ter ocorrido e TEM completion registrada (isso limita a
    // janela) -- dias em que o habito nao deveria ocorrer sao so pulados, nao
    // interrompem a busca. Retorna null se nao ha nenhum dia esquecido dentro
    // da janela de MAX_LOOKBACK dias.
    private Integer daysSinceForgotten(AgendaEvent habit, LocalDate today) {
        LocalDate earliestMissed = null;
        LocalDate date = today.minusDays(1);

        for (int scanned = 0; scanned < HABIT_FORGOTTEN_MAX_LOOKBACK_DAYS; scanned++) {
            if (habitOccursOn(habit, date)) {
                boolean hasCompletion = agendaEventCompletionRepository
                        .findByAgendaEvent_IdAndEventDate(habit.getId(), date)
                        .isPresent();
                if (hasCompletion) {
                    break;
                }
                earliestMissed = date;
            }
            date = date.minusDays(1);
        }

        return earliestMissed == null ? null : (int) ChronoUnit.DAYS.between(earliestMissed, today);
    }

    // ===== Resumos sob demanda (POST /agenda/notify/*): reaproveitam a mesma
    // logica de consulta/montagem dos jobs automaticos acima, mas disparam na
    // hora (sem esperar cron nem checar NotificationLog) e SEMPRE mandam uma
    // mensagem -- mesmo vazia -- porque foram um pedido explicito do usuario;
    // ao contrario dos jobs automaticos, ficar em silencio pareceria que nao
    // fez nada. Retornam a quantidade de itens incluidos na mensagem.

    public int notifyToday() {
        LocalDate today = LocalDate.now(ZONE);

        List<AgendaEvent> oneTimeToday = oneTimeEventsOn(today);
        List<AgendaEvent> habitsToday = habitsOn(today);
        List<Bill> billsToday = billRepository.findAllByMaturityAndStatus(today, StatusBill.PENDING);

        int itemCount = oneTimeToday.size() + habitsToday.size() + billsToday.size();

        if (itemCount == 0) {
            whatsAppNotificationService.sendMessage("📆 Hoje: nada agendado e nenhuma conta vencendo. 🎉");
            return 0;
        }

        StringBuilder sb = new StringBuilder("📆 Hoje:\n");
        appendEventAndHabitLines(sb, oneTimeToday, habitsToday);
        billsToday.forEach(b -> sb.append("\n💰 ").append(b.getDescription())
                .append(" — ").append(formatCurrency(b.getInstallmentAmount())));

        whatsAppNotificationService.sendMessage(sb.toString());
        return itemCount;
    }

    public int notifyWeek() {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate start = today.plusDays(1);
        LocalDate end = today.plusDays(7);

        List<Bill> bills = billRepository.findAllByMaturityBetweenAndStatus(start, end, StatusBill.PENDING);

        if (bills.isEmpty()) {
            whatsAppNotificationService.sendMessage("📅 Próximos 7 dias: nenhuma conta vencendo. 🎉");
            return 0;
        }

        StringBuilder sb = new StringBuilder("📅 Próximos 7 dias:\n");
        bills.stream()
                .sorted((a, b) -> a.getMaturity().compareTo(b.getMaturity()))
                .forEach(b -> sb.append("\n💰 ").append(b.getMaturity().format(DATE_FMT))
                        .append(" — ").append(b.getDescription())
                        .append(" — ").append(formatCurrency(b.getInstallmentAmount())));

        whatsAppNotificationService.sendMessage(sb.toString());
        return bills.size();
    }

    public int notifyOpenBills() {
        List<Bill> bills = billRepository.findAllByStatus(StatusBill.PENDING);

        if (bills.isEmpty()) {
            whatsAppNotificationService.sendMessage("💰 Tudo em aberto: nenhuma conta pendente. 🎉");
            return 0;
        }

        StringBuilder sb = new StringBuilder("💰 Tudo em aberto:\n");
        bills.stream()
                .sorted((a, b) -> a.getMaturity().compareTo(b.getMaturity()))
                .forEach(b -> sb.append("\n💰 ").append(b.getMaturity().format(DATE_FMT_FULL))
                        .append(" — ").append(b.getDescription())
                        .append(" — ").append(formatCurrency(b.getInstallmentAmount())));

        whatsAppNotificationService.sendMessage(sb.toString());
        return bills.size();
    }

    private void markSent(String jobKey, LocalDate referenceDate) {
        NotificationLog entry = new NotificationLog();
        entry.setJobKey(jobKey);
        entry.setReferenceDate(referenceDate);
        entry.setSentAt(LocalDateTime.now(ZONE));
        notificationLogRepository.save(entry);
    }
}
