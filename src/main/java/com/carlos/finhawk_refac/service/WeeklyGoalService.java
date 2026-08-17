package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.dto.request.WeeklyGoalRequestDTO;
import com.carlos.finhawk_refac.dto.response.WeeklyGoalResponseDTO;
import com.carlos.finhawk_refac.entity.Account;
import com.carlos.finhawk_refac.entity.UserAccount;
import com.carlos.finhawk_refac.entity.WeeklyGoal;
import com.carlos.finhawk_refac.repository.AccountRepository;
import com.carlos.finhawk_refac.repository.WeeklyGoalRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class WeeklyGoalService {

    // Mesmo fuso usado no resto da agenda (schedulers, AgendaEventService).
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private final WeeklyGoalRepository weeklyGoalRepository;
    private final AccountRepository accountRepository;
    private final AuditLogService auditLogService;
    private final CrudNotificationService crudNotificationService;

    public WeeklyGoalService(WeeklyGoalRepository weeklyGoalRepository,
                              AccountRepository accountRepository,
                              AuditLogService auditLogService,
                              CrudNotificationService crudNotificationService) {
        this.weeklyGoalRepository = weeklyGoalRepository;
        this.accountRepository = accountRepository;
        this.auditLogService = auditLogService;
        this.crudNotificationService = crudNotificationService;
    }

    public static LocalDate currentWeekMonday() {
        return LocalDate.now(ZONE).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private UserAccount getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserAccount)) {
            throw new RuntimeException("Unauthenticated user");
        }

        return (UserAccount) authentication.getPrincipal();
    }

    private WeeklyGoalResponseDTO toResponseDTO(WeeklyGoal goal) {
        return new WeeklyGoalResponseDTO(
                goal.getId(),
                goal.getTitle(),
                goal.getAccount().getId(),
                goal.getWeekStartDate(),
                goal.getCompleted(),
                goal.getCreatedAt(),
                goal.getUpdatedAt()
        );
    }

    private WeeklyGoal findOwned(Long id, UserAccount currentUser) {
        WeeklyGoal goal = weeklyGoalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Weekly goal not found"));

        if (!goal.getAccount().getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        return goal;
    }

    @Transactional
    public WeeklyGoalResponseDTO create(WeeklyGoalRequestDTO dto) {
        UserAccount currentUser = getAuthenticatedUser();

        Account account = accountRepository.findById(dto.accountId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to use this account");
        }

        WeeklyGoal goal = new WeeklyGoal();
        goal.setTitle(dto.title());
        goal.setAccount(account);
        goal.setWeekStartDate(currentWeekMonday());
        goal.setCompleted(false);

        WeeklyGoal saved = weeklyGoalRepository.save(goal);

        auditLogService.record(currentUser, AuditLogService.CREATE, "WeeklyGoal", saved.getId(), saved.getTitle());
        crudNotificationService.notify("🎯 Nova meta da semana\n📝 " + saved.getTitle());

        return toResponseDTO(saved);
    }

    public List<WeeklyGoalResponseDTO> getCurrentWeek(Long accountId) {
        UserAccount currentUser = getAuthenticatedUser();

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        return weeklyGoalRepository.findAllByAccount_IdAndWeekStartDate(accountId, currentWeekMonday())
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional
    public WeeklyGoalResponseDTO setCompleted(Long id, boolean completed) {
        UserAccount currentUser = getAuthenticatedUser();
        WeeklyGoal goal = findOwned(id, currentUser);

        boolean wasCompleted = Boolean.TRUE.equals(goal.getCompleted());
        goal.setCompleted(completed);
        WeeklyGoal updated = weeklyGoalRepository.save(goal);

        auditLogService.record(currentUser, AuditLogService.UPDATE, "WeeklyGoal", updated.getId(),
                updated.getTitle() + " -> completed=" + completed);

        // So comemora ao concluir -- reabrir (desmarcar por engano) e so uma
        // correcao, sem notificacao, mesmo espirito de "pulado" nao notificar
        // pra habito.
        if (!wasCompleted && completed) {
            crudNotificationService.notify("✅ Meta concluída!\n📝 " + updated.getTitle());
        }

        return toResponseDTO(updated);
    }

    @Transactional
    public void delete(Long id) {
        UserAccount currentUser = getAuthenticatedUser();
        WeeklyGoal goal = findOwned(id, currentUser);

        weeklyGoalRepository.delete(goal);

        auditLogService.record(currentUser, AuditLogService.DELETE, "WeeklyGoal", goal.getId(), goal.getTitle());
        crudNotificationService.notify("🗑️ Meta removida\n📝 " + goal.getTitle());
    }
}
