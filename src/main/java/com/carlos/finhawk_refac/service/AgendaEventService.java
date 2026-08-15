package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.dto.request.AgendaEventCompletionRequestDTO;
import com.carlos.finhawk_refac.dto.request.AgendaEventRequestDTO;
import com.carlos.finhawk_refac.dto.response.AgendaEventCompletionResponseDTO;
import com.carlos.finhawk_refac.dto.response.AgendaEventResponseDTO;
import com.carlos.finhawk_refac.entity.Account;
import com.carlos.finhawk_refac.entity.AgendaEvent;
import com.carlos.finhawk_refac.entity.AgendaEventCompletion;
import com.carlos.finhawk_refac.entity.UserAccount;
import com.carlos.finhawk_refac.enums.AgendaEventType;
import com.carlos.finhawk_refac.enums.RecurrenceFrequency;
import com.carlos.finhawk_refac.repository.AccountRepository;
import com.carlos.finhawk_refac.repository.AgendaEventCompletionRepository;
import com.carlos.finhawk_refac.repository.AgendaEventRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class AgendaEventService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Map<DayOfWeek, String> DAY_ABBREV = Map.of(
            DayOfWeek.MONDAY, "Seg", DayOfWeek.TUESDAY, "Ter", DayOfWeek.WEDNESDAY, "Qua",
            DayOfWeek.THURSDAY, "Qui", DayOfWeek.FRIDAY, "Sex", DayOfWeek.SATURDAY, "Sáb",
            DayOfWeek.SUNDAY, "Dom"
    );

    private final AgendaEventRepository agendaEventRepository;
    private final AccountRepository accountRepository;
    private final AgendaEventCompletionRepository agendaEventCompletionRepository;
    private final AuditLogService auditLogService;
    private final CrudNotificationService crudNotificationService;

    public AgendaEventService(AgendaEventRepository agendaEventRepository,
                               AccountRepository accountRepository,
                               AgendaEventCompletionRepository agendaEventCompletionRepository,
                               AuditLogService auditLogService,
                               CrudNotificationService crudNotificationService) {
        this.agendaEventRepository = agendaEventRepository;
        this.accountRepository = accountRepository;
        this.agendaEventCompletionRepository = agendaEventCompletionRepository;
        this.auditLogService = auditLogService;
        this.crudNotificationService = crudNotificationService;
    }

    // ===== Mensagens de notificacao (formato completo, aprovado pelo usuario) =====

    private String habitScheduleFor(RecurrenceFrequency frequency, java.time.LocalTime timeOfDay, List<DayOfWeek> daysOfWeek) {
        if (timeOfDay == null) {
            return "-";
        }
        String time = timeOfDay.format(TIME_FMT);
        if (frequency == RecurrenceFrequency.WEEKLY) {
            String days = daysOfWeek == null ? "" : daysOfWeek.stream()
                    .sorted()
                    .map(DAY_ABBREV::get)
                    .reduce((a, b) -> a + ", " + b).orElse("");
            return days + " às " + time;
        }
        return "Todo dia às " + time;
    }

    private String habitSchedule(AgendaEvent habit) {
        return habitScheduleFor(habit.getRecurrenceFrequency(), habit.getTimeOfDay(), habit.getDaysOfWeek());
    }

    private String buildEventCreatedMessage(AgendaEvent event) {
        StringBuilder sb = new StringBuilder("🆕 Novo evento na agenda");
        sb.append("\n📌 ").append(event.getTitle());
        if (event.getDescription() != null && !event.getDescription().isBlank()) {
            sb.append("\n📝 ").append(event.getDescription());
        }
        sb.append("\n📅 ").append(event.getEventDateTime().format(DATE_FMT))
                .append(" às ").append(event.getEventDateTime().format(TIME_FMT));
        return sb.toString();
    }

    private String buildHabitCreatedMessage(AgendaEvent habit) {
        return "🆕 Novo hábito\n🔁 " + habit.getTitle() + "\n⏰ " + habitSchedule(habit);
    }

    private String buildEventUpdatedMessage(AgendaEvent updated, LocalDateTime oldEventDateTime) {
        StringBuilder sb = new StringBuilder("✏️ Evento atualizado");
        sb.append("\n📌 ").append(updated.getTitle());
        sb.append("\n📅 ");
        if (oldEventDateTime != null) {
            sb.append(oldEventDateTime.format(DATE_FMT)).append(" às ").append(oldEventDateTime.format(TIME_FMT));
        } else {
            sb.append("-");
        }
        sb.append(" → ").append(updated.getEventDateTime().format(DATE_FMT))
                .append(" às ").append(updated.getEventDateTime().format(TIME_FMT));
        return sb.toString();
    }

    private String buildHabitUpdatedMessage(AgendaEvent updated, RecurrenceFrequency oldFrequency,
                                             java.time.LocalTime oldTimeOfDay, List<DayOfWeek> oldDaysOfWeek) {
        String oldSchedule = habitScheduleFor(oldFrequency, oldTimeOfDay, oldDaysOfWeek);
        String newSchedule = habitSchedule(updated);
        return "✏️ Hábito atualizado\n🔁 " + updated.getTitle() + "\n⏰ " + oldSchedule + " → " + newSchedule;
    }

    private UserAccount getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserAccount)) {
            throw new RuntimeException("Unauthenticated user");
        }

        return (UserAccount) authentication.getPrincipal();
    }

    private AgendaEventResponseDTO toResponseDTO(AgendaEvent event) {
        return new AgendaEventResponseDTO(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getAccount().getId(),
                event.getType(),
                event.getEventDateTime(),
                event.getRecurrenceFrequency(),
                event.getDaysOfWeek(),
                event.getTimeOfDay(),
                event.getActive(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }

    private AgendaEventCompletionResponseDTO toCompletionResponseDTO(AgendaEventCompletion c) {
        return new AgendaEventCompletionResponseDTO(
                c.getId(),
                c.getAgendaEvent().getId(),
                c.getEventDate(),
                c.getStatus(),
                c.getCompletedAt()
        );
    }

    private void validateTypeSpecificFields(AgendaEventType type, AgendaEventRequestDTO dto) {
        if (type == AgendaEventType.ONE_TIME) {
            if (dto.eventDateTime() == null) {
                throw new RuntimeException("A data e hora do evento são obrigatórias para eventos pontuais.");
            }
        } else if (type == AgendaEventType.HABIT) {
            if (dto.recurrenceFrequency() == null) {
                throw new RuntimeException("A frequência de recorrência é obrigatória para hábitos.");
            }
            if (dto.timeOfDay() == null) {
                throw new RuntimeException("O horário do lembrete é obrigatório para hábitos.");
            }
            if (dto.recurrenceFrequency() == RecurrenceFrequency.WEEKLY
                    && (dto.daysOfWeek() == null || dto.daysOfWeek().isEmpty())) {
                throw new RuntimeException("Selecione ao menos um dia da semana para hábitos semanais.");
            }
        }
    }

    @Transactional
    public AgendaEventResponseDTO create(AgendaEventRequestDTO dto) {
        UserAccount currentUser = getAuthenticatedUser();

        if (dto.title() == null || dto.title().isBlank()) {
            throw new RuntimeException("O título é obrigatório.");
        }

        if (dto.type() == null) {
            throw new RuntimeException("O tipo do evento é obrigatório.");
        }

        if (dto.accountId() == null) {
            throw new RuntimeException("Account not found");
        }

        Account account = accountRepository.findById(dto.accountId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to use this account");
        }

        validateTypeSpecificFields(dto.type(), dto);

        AgendaEvent event = new AgendaEvent();
        event.setTitle(dto.title());
        event.setDescription(dto.description());
        event.setAccount(account);
        event.setType(dto.type());
        event.setActive(dto.active() != null ? dto.active() : true);

        if (dto.type() == AgendaEventType.ONE_TIME) {
            event.setEventDateTime(dto.eventDateTime());
        } else {
            event.setRecurrenceFrequency(dto.recurrenceFrequency());
            event.setTimeOfDay(dto.timeOfDay());
            event.setDaysOfWeek(dto.recurrenceFrequency() == RecurrenceFrequency.WEEKLY ? dto.daysOfWeek() : null);
        }

        AgendaEvent saved = agendaEventRepository.save(event);

        auditLogService.record(currentUser, AuditLogService.CREATE, "AgendaEvent", saved.getId(), saved.getTitle());
        crudNotificationService.notify(saved.getType() == AgendaEventType.ONE_TIME
                ? buildEventCreatedMessage(saved)
                : buildHabitCreatedMessage(saved));

        return toResponseDTO(saved);
    }

    private AgendaEvent findOwned(Long id, UserAccount currentUser) {
        AgendaEvent event = agendaEventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agenda event not found"));

        if (event.getDeletedAt() != null) {
            throw new RuntimeException("Agenda event not found");
        }

        if (!event.getAccount().getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        return event;
    }

    @Transactional
    public AgendaEventResponseDTO update(Long id, AgendaEventRequestDTO dto) {
        UserAccount currentUser = getAuthenticatedUser();

        AgendaEvent event = findOwned(id, currentUser);
        boolean wasActive = event.getActive();
        LocalDateTime oldEventDateTime = event.getEventDateTime();
        RecurrenceFrequency oldRecurrenceFrequency = event.getRecurrenceFrequency();
        java.time.LocalTime oldTimeOfDay = event.getTimeOfDay();
        List<DayOfWeek> oldDaysOfWeek = event.getDaysOfWeek();

        if (dto.title() != null && !dto.title().isBlank()) {
            event.setTitle(dto.title());
        }

        if (dto.description() != null) {
            event.setDescription(dto.description());
        }

        if (dto.active() != null) {
            event.setActive(dto.active());
        }

        AgendaEventType effectiveType = dto.type() != null ? dto.type() : event.getType();

        if (dto.type() != null) {
            event.setType(dto.type());
        }

        if (effectiveType == AgendaEventType.ONE_TIME) {
            if (dto.eventDateTime() != null) {
                event.setEventDateTime(dto.eventDateTime());
                // Data alterada: reabre a janela de lembrete "1h antes".
                event.setReminderSentAt(null);
            }
            if (dto.type() != null) {
                event.setRecurrenceFrequency(null);
                event.setTimeOfDay(null);
                event.setDaysOfWeek(null);
            }
        } else {
            if (dto.recurrenceFrequency() != null) {
                event.setRecurrenceFrequency(dto.recurrenceFrequency());
            }
            if (dto.timeOfDay() != null) {
                event.setTimeOfDay(dto.timeOfDay());
            }
            if (dto.daysOfWeek() != null) {
                event.setDaysOfWeek(dto.daysOfWeek());
            }
            if (event.getRecurrenceFrequency() != RecurrenceFrequency.WEEKLY) {
                event.setDaysOfWeek(null);
            }
            if (dto.type() != null) {
                event.setEventDateTime(null);
            }
        }

        AgendaEvent updated = agendaEventRepository.save(event);

        auditLogService.record(currentUser, AuditLogService.UPDATE, "AgendaEvent", updated.getId(), updated.getTitle());

        // Prioridade: 1) active mudou -> so pausar/reativar (nunca junto com
        // "editado", seria redundante); 2) agendamento mudou (data/hora do
        // evento, ou frequencia/horario/dias do habito) -> editado; 3) so
        // titulo/descricao mudaram (ou nada mudou de fato) -> nao notifica,
        // nada relevante pro grupo saber.
        if (wasActive != updated.getActive()) {
            crudNotificationService.notify(updated.getActive()
                    ? "▶️ Hábito reativado\n🔁 " + updated.getTitle()
                    : "⏸️ Hábito pausado\n🔁 " + updated.getTitle());
        } else if (updated.getType() == AgendaEventType.ONE_TIME) {
            boolean dateTimeChanged = !java.util.Objects.equals(oldEventDateTime, updated.getEventDateTime());
            if (dateTimeChanged) {
                crudNotificationService.notify(buildEventUpdatedMessage(updated, oldEventDateTime));
            }
        } else {
            boolean scheduleChanged = !java.util.Objects.equals(oldRecurrenceFrequency, updated.getRecurrenceFrequency())
                    || !java.util.Objects.equals(oldTimeOfDay, updated.getTimeOfDay())
                    || !java.util.Objects.equals(oldDaysOfWeek, updated.getDaysOfWeek());

            if (scheduleChanged) {
                crudNotificationService.notify(buildHabitUpdatedMessage(updated, oldRecurrenceFrequency, oldTimeOfDay, oldDaysOfWeek));
            }
        }

        return toResponseDTO(updated);
    }

    public List<AgendaEventResponseDTO> getAllByAccountId(Long accountId, AgendaEventType type) {
        UserAccount currentUser = getAuthenticatedUser();

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        List<AgendaEvent> events = type != null
                ? agendaEventRepository.findAllByAccount_IdAndTypeAndDeletedAtIsNull(accountId, type)
                : agendaEventRepository.findAllByAccount_IdAndDeletedAtIsNull(accountId);

        return events.stream().map(this::toResponseDTO).toList();
    }

    @Transactional
    public void delete(Long id) {
        UserAccount currentUser = getAuthenticatedUser();

        AgendaEvent event = findOwned(id, currentUser);

        event.setDeletedAt(LocalDateTime.now());
        agendaEventRepository.save(event);

        auditLogService.record(currentUser, AuditLogService.DELETE, "AgendaEvent", event.getId(), event.getTitle());
        crudNotificationService.notify(event.getType() == AgendaEventType.ONE_TIME
                ? "🗑️ Evento removido\n📌 " + event.getTitle()
                : "🗑️ Hábito removido\n🔁 " + event.getTitle());
    }

    // ===== Conclusão diária (hábitos) =====

    public record MarkCompletionResult(AgendaEventCompletionResponseDTO dto, boolean created) {}

    @Transactional
    public MarkCompletionResult markCompletion(Long eventId, AgendaEventCompletionRequestDTO dto) {
        if (dto.eventDate() == null) {
            throw new RuntimeException("A data é obrigatória.");
        }
        if (dto.status() == null) {
            throw new RuntimeException("O status é obrigatório.");
        }

        UserAccount currentUser = getAuthenticatedUser();
        AgendaEvent event = findOwned(eventId, currentUser);

        Optional<AgendaEventCompletion> existing = agendaEventCompletionRepository
                .findByAgendaEvent_IdAndEventDate(eventId, dto.eventDate());

        if (existing.isPresent()) {
            AgendaEventCompletion completion = existing.get();
            completion.setStatus(dto.status());
            completion.setCompletedAt(LocalDateTime.now());
            return new MarkCompletionResult(toCompletionResponseDTO(agendaEventCompletionRepository.save(completion)), false);
        }

        AgendaEventCompletion completion = new AgendaEventCompletion();
        completion.setAgendaEvent(event);
        completion.setEventDate(dto.eventDate());
        completion.setStatus(dto.status());
        completion.setCompletedAt(LocalDateTime.now());

        return new MarkCompletionResult(toCompletionResponseDTO(agendaEventCompletionRepository.save(completion)), true);
    }

    @Transactional
    public void unmarkCompletion(Long eventId, java.time.LocalDate eventDate) {
        UserAccount currentUser = getAuthenticatedUser();
        findOwned(eventId, currentUser);

        agendaEventCompletionRepository
                .findByAgendaEvent_IdAndEventDate(eventId, eventDate)
                .ifPresent(agendaEventCompletionRepository::delete);
    }

    public List<AgendaEventCompletionResponseDTO> getCompletionsByDate(Long accountId, java.time.LocalDate eventDate) {
        UserAccount currentUser = getAuthenticatedUser();

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        return agendaEventCompletionRepository.findAllByAgendaEvent_Account_IdAndEventDate(accountId, eventDate)
                .stream()
                .map(this::toCompletionResponseDTO)
                .toList();
    }
}
