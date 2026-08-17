package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.entity.Account;
import com.carlos.finhawk_refac.entity.AgendaEvent;
import com.carlos.finhawk_refac.entity.Bill;
import com.carlos.finhawk_refac.enums.DayType;
import com.carlos.finhawk_refac.enums.Periodicity;
import com.carlos.finhawk_refac.enums.RecurrenceFrequency;
import com.carlos.finhawk_refac.enums.StatusBill;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Monta o bloco de campos completos de um Bill/AgendaEvent (todo campo
// preenchido, sem resumir) pra notificacao WhatsApp -- usado tanto nas
// mensagens de CRUD (BillService, AgendaEventService) quanto nos resumos
// sob demanda (AgendaNotificationScheduler), pra manter os dois pontos de
// entrada sempre mostrando exatamente os mesmos campos, no mesmo formato.
public final class NotificationMessageBuilder {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Map<DayOfWeek, String> DAY_ABBREV = Map.of(
            DayOfWeek.MONDAY, "Seg", DayOfWeek.TUESDAY, "Ter", DayOfWeek.WEDNESDAY, "Qua",
            DayOfWeek.THURSDAY, "Qui", DayOfWeek.FRIDAY, "Sex", DayOfWeek.SATURDAY, "Sáb",
            DayOfWeek.SUNDAY, "Dom"
    );

    private NotificationMessageBuilder() {
    }

    public static String formatCurrency(BigDecimal value) {
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return fmt.format(value != null ? value : BigDecimal.ZERO);
    }

    public static String formatDate(LocalDate date) {
        return date == null ? "-" : date.format(DATE_FMT);
    }

    public static String statusLabel(StatusBill status) {
        return switch (status) {
            case PENDING -> "Pendente";
            case PAID -> "Pago";
            case RECEIVED -> "Recebido";
        };
    }

    public static String periodicityLabel(Periodicity periodicity) {
        if (periodicity == null) {
            return "-";
        }
        return switch (periodicity) {
            case MONTHLY -> "Mensal";
            case BIMONTHLY -> "Bimestral";
            case QUARTERLY -> "Trimestral";
            case SEMIANNUAL -> "Semestral";
            case ANNUAL -> "Anual";
        };
    }

    public static String dayTypeLabel(DayType dayType) {
        return switch (dayType) {
            case PLANTAO -> "Plantão";
            case FOLGA -> "Folga";
            case ENTREGA -> "Entrega";
            case FIM_DE_SEMANA -> "Fim de semana";
        };
    }

    // Etiquetas de tipo de dia (se o habito usar esse modo -- tem prioridade,
    // ver DayTypeService), senao "Todo dia" (DAILY) ou os dias abreviados em
    // ordem (segunda-first), independente da ordem em que foram salvos no banco.
    public static String habitFrequencyLabel(RecurrenceFrequency frequency, List<DayOfWeek> daysOfWeek,
                                              List<DayType> dayTypeTags) {
        if (dayTypeTags != null && !dayTypeTags.isEmpty()) {
            return dayTypeTags.stream()
                    .map(NotificationMessageBuilder::dayTypeLabel)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("-");
        }

        if (frequency == RecurrenceFrequency.WEEKLY) {
            if (daysOfWeek == null || daysOfWeek.isEmpty()) {
                return "-";
            }
            return daysOfWeek.stream()
                    .sorted()
                    .map(DAY_ABBREV::get)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("-");
        }
        return "Todo dia";
    }

    // ===== Lancamento (Bill) =====
    // amountLineOverride/maturityLineOverride: usados pela mensagem de
    // "editado" pra mostrar "antes -> depois" so no(s) campo(s) que
    // realmente mudou(aram); os demais campos continuam no valor atual.

    public static void appendBillContext(StringBuilder sb, Bill bill, String amountLineOverride, String maturityLineOverride) {
        sb.append("\n📝 Descrição: ").append(bill.getDescription());

        if (bill.getCategory() != null && bill.getCategory().getName() != null) {
            sb.append("\n🏷️ Categoria: ").append(bill.getCategory().getName());
        }

        sb.append("\n").append(amountLineOverride != null
                ? amountLineOverride
                : "💰 Valor: " + formatCurrency(bill.getInstallmentAmount()));

        // Parcela/periodicidade so fazem sentido pra um lancamento que
        // realmente faz parte de uma serie de parcelas -- periodicity e
        // obrigatorio em todo Bill, mas so tem efeito real quando
        // installmentCount > 1 (ver BillService.create()).
        if (bill.getInstallmentCount() != null && bill.getInstallmentCount() > 1) {
            sb.append("\n🔢 Parcela: ").append(bill.getCurrentInstallment()).append("/").append(bill.getInstallmentCount());
            sb.append("\n🔁 Periodicidade: ").append(periodicityLabel(bill.getPeriodicity()));
        }

        if (bill.getEmission() != null && !bill.getEmission().equals(bill.getMaturity())) {
            sb.append("\n📅 Emissão: ").append(formatDate(bill.getEmission()));
        }

        sb.append("\n").append(maturityLineOverride != null
                ? maturityLineOverride
                : "📅 Vencimento: " + formatDate(bill.getMaturity()));

        sb.append("\n📌 Status: ").append(statusLabel(bill.getStatus()));

        Account account = bill.getAccount();
        if (account != null && account.getName() != null) {
            sb.append("\n💳 Conta: ").append(account.getName());
        }

        if (bill.getStatus() == StatusBill.PAID && bill.getPaidAt() != null) {
            sb.append("\n✅ Pago em: ").append(formatDate(bill.getPaidAt().toLocalDate()));
        }
        if (bill.getStatus() == StatusBill.RECEIVED && bill.getReceivedAt() != null) {
            sb.append("\n💰 Recebido em: ").append(formatDate(bill.getReceivedAt().toLocalDate()));
        }
    }

    public static String billMessage(String header, Bill bill) {
        StringBuilder sb = new StringBuilder(header);
        appendBillContext(sb, bill, null, null);
        return sb.toString();
    }

    // Bloco de campos sem cabecalho, pra uso em listas (resumos sob demanda).
    public static String billBlock(Bill bill) {
        StringBuilder sb = new StringBuilder();
        appendBillContext(sb, bill, null, null);
        return sb.substring(1);
    }

    // ===== Evento pontual (ONE_TIME) =====

    public static void appendEventContext(StringBuilder sb, AgendaEvent event, String dateLineOverride, String timeLineOverride) {
        sb.append("\n📌 Título: ").append(event.getTitle());
        if (event.getDescription() != null && !event.getDescription().isBlank()) {
            sb.append("\n📝 Descrição: ").append(event.getDescription());
        }
        sb.append("\n").append(dateLineOverride != null
                ? dateLineOverride
                : "📅 Data: " + formatDate(event.getEventDateTime().toLocalDate()));
        sb.append("\n").append(timeLineOverride != null
                ? timeLineOverride
                : "🕒 Hora: " + event.getEventDateTime().toLocalTime().format(TIME_FMT));
        // "Concluido" nao existe como campo proprio do evento (a conclusao e
        // rastreada a parte, por data, em AgendaEventCompletion) -- aqui so
        // reflete o campo "active" que de fato existe na entidade.
        sb.append("\n📌 Status: ").append(Boolean.TRUE.equals(event.getActive()) ? "Ativo" : "Inativo");
    }

    public static String eventMessage(String header, AgendaEvent event) {
        StringBuilder sb = new StringBuilder(header);
        appendEventContext(sb, event, null, null);
        return sb.toString();
    }

    public static String eventBlock(AgendaEvent event) {
        StringBuilder sb = new StringBuilder();
        appendEventContext(sb, event, null, null);
        return sb.substring(1);
    }

    // ===== Habito (HABIT) =====

    public static void appendHabitContext(StringBuilder sb, AgendaEvent habit, String frequencyLineOverride, String timeLineOverride) {
        sb.append("\n🔁 Título: ").append(habit.getTitle());
        if (habit.getDescription() != null && !habit.getDescription().isBlank()) {
            sb.append("\n📝 Descrição: ").append(habit.getDescription());
        }
        sb.append("\n").append(frequencyLineOverride != null
                ? frequencyLineOverride
                : "⏰ Frequência: " + habitFrequencyLabel(habit.getRecurrenceFrequency(), habit.getDaysOfWeek(), habit.getDayTypeTags()));
        sb.append("\n").append(timeLineOverride != null
                ? timeLineOverride
                : "🕒 Horário: " + (habit.getTimeOfDay() != null ? habit.getTimeOfDay().format(TIME_FMT) : "-"));
        sb.append("\n📌 Status: ").append(Boolean.TRUE.equals(habit.getActive()) ? "Ativo" : "Pausado");
    }

    public static String habitMessage(String header, AgendaEvent habit) {
        StringBuilder sb = new StringBuilder(header);
        appendHabitContext(sb, habit, null, null);
        return sb.toString();
    }

    public static String habitBlock(AgendaEvent habit) {
        StringBuilder sb = new StringBuilder();
        appendHabitContext(sb, habit, null, null);
        return sb.substring(1);
    }
}
