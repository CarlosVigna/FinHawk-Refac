package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.dto.request.ChecklistItemRequestDTO;
import com.carlos.finhawk_refac.dto.response.ChecklistCompletionResponseDTO;
import com.carlos.finhawk_refac.dto.response.ChecklistItemResponseDTO;
import com.carlos.finhawk_refac.dto.response.ChecklistSuggestionDTO;
import com.carlos.finhawk_refac.entity.Account;
import com.carlos.finhawk_refac.entity.Bill;
import com.carlos.finhawk_refac.entity.ChecklistCompletion;
import com.carlos.finhawk_refac.entity.ChecklistItem;
import com.carlos.finhawk_refac.entity.UserAccount;
import com.carlos.finhawk_refac.repository.AccountRepository;
import com.carlos.finhawk_refac.repository.BillRepository;
import com.carlos.finhawk_refac.repository.ChecklistCompletionRepository;
import com.carlos.finhawk_refac.repository.ChecklistItemRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ChecklistItemService {

    private final ChecklistItemRepository checklistItemRepository;
    private final AccountRepository accountRepository;
    private final BillRepository billRepository;
    private final ChecklistCompletionRepository checklistCompletionRepository;
    private final AuditLogService auditLogService;

    public ChecklistItemService(ChecklistItemRepository checklistItemRepository,
                                AccountRepository accountRepository,
                                BillRepository billRepository,
                                ChecklistCompletionRepository checklistCompletionRepository,
                                AuditLogService auditLogService) {
        this.checklistItemRepository = checklistItemRepository;
        this.accountRepository = accountRepository;
        this.billRepository = billRepository;
        this.checklistCompletionRepository = checklistCompletionRepository;
        this.auditLogService = auditLogService;
    }

    private UserAccount getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserAccount)) {
            throw new RuntimeException("Unauthenticated user");
        }

        return (UserAccount) authentication.getPrincipal();
    }

    private ChecklistItemResponseDTO toResponseDTO(ChecklistItem item) {
        return new ChecklistItemResponseDTO(
                item.getId(),
                item.getDescription(),
                item.getDueDay(),
                item.getActive(),
                item.getAccount().getId(),
                item.getApproximateValue()
        );
    }

    private ChecklistCompletionResponseDTO toCompletionResponseDTO(ChecklistCompletion c) {
        return new ChecklistCompletionResponseDTO(
                c.getId(),
                c.getChecklistItem().getId(),
                c.getMonth(),
                c.getCompletedAt()
        );
    }

    private void validateMonth(String month) {
        if (month == null || !month.matches("^\\d{4}-(0[1-9]|1[0-2])$")) {
            throw new RuntimeException("Formato de mês inválido. Esperado: YYYY-MM (ex: 2026-06)");
        }
    }

    @Transactional
    public ChecklistItemResponseDTO create(ChecklistItemRequestDTO dto) {
        UserAccount currentUser = getAuthenticatedUser();

        if (dto.description() == null || dto.description().isBlank()) {
            throw new RuntimeException("A descrição é obrigatória.");
        }

        if (dto.dueDay() == null) {
            throw new RuntimeException("O dia do vencimento é obrigatório.");
        }

        if (dto.accountId() == null) {
            throw new RuntimeException("Account not found");
        }

        Account account = accountRepository.findById(dto.accountId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to use this account");
        }

        ChecklistItem item = new ChecklistItem();
        item.setDescription(dto.description());
        item.setDueDay(dto.dueDay());
        item.setActive(dto.active() != null ? dto.active() : true);
        item.setAccount(account);
        item.setApproximateValue(dto.approximateValue());

        ChecklistItem saved = checklistItemRepository.save(item);

        auditLogService.record(currentUser, AuditLogService.CREATE, "ChecklistItem", saved.getId(), saved.getDescription());

        return toResponseDTO(saved);
    }

    @Transactional
    public ChecklistItemResponseDTO update(Long id, ChecklistItemRequestDTO dto) {
        UserAccount currentUser = getAuthenticatedUser();

        ChecklistItem item = checklistItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Checklist item not found"));

        if (!item.getAccount().getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        if (dto.description() != null && !dto.description().isBlank()) {
            item.setDescription(dto.description());
        }

        if (dto.dueDay() != null) {
            item.setDueDay(dto.dueDay());
        }

        if (dto.active() != null) {
            item.setActive(dto.active());
        }

        ChecklistItem updated = checklistItemRepository.save(item);

        auditLogService.record(currentUser, AuditLogService.UPDATE, "ChecklistItem", updated.getId(), updated.getDescription());

        return toResponseDTO(updated);
    }

    public List<ChecklistItemResponseDTO> getAllByAccountId(Long accountId) {
        UserAccount currentUser = getAuthenticatedUser();

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        return checklistItemRepository.findAllByAccount_IdAndActiveTrueOrderByDueDayAsc(accountId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        UserAccount currentUser = getAuthenticatedUser();

        ChecklistItem item = checklistItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Checklist item not found"));

        if (!item.getAccount().getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        item.setActive(false);
        checklistItemRepository.save(item);

        auditLogService.record(currentUser, AuditLogService.DELETE, "ChecklistItem", item.getId(), item.getDescription());
    }

    public ChecklistSuggestionDTO getSuggestion(Long checklistId) {
        UserAccount currentUser = getAuthenticatedUser();

        ChecklistItem item = checklistItemRepository.findById(checklistId)
                .orElseThrow(() -> new RuntimeException("Checklist item not found"));

        if (!item.getAccount().getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        List<Bill> bills = billRepository.findAllByAccount_Id(item.getAccount().getId());

        return bills.stream()
                .filter(b -> b.getDescription() != null
                        && b.getDescription().toLowerCase().contains(item.getDescription().toLowerCase()))
                .max(Comparator.comparing(Bill::getMaturity))
                .map(b -> new ChecklistSuggestionDTO(
                        b.getInstallmentAmount(),
                        b.getCategory() != null ? b.getCategory().getId() : null,
                        b.getMaturity(),
                        b.getDescription(),
                        item.getApproximateValue()
                ))
                .orElse(new ChecklistSuggestionDTO(null, null, null, null, item.getApproximateValue()));
    }

    // ===== Conclusão mensal =====

    public record MarkCompletionResult(ChecklistCompletionResponseDTO dto, boolean created) {}

    @Transactional
    public MarkCompletionResult markComplete(Long itemId, String month) {
        validateMonth(month);
        UserAccount currentUser = getAuthenticatedUser();

        ChecklistItem item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Checklist item not found"));

        if (!item.getAccount().getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        if (Boolean.FALSE.equals(item.getActive())) {
            throw new RuntimeException("Checklist item not found");
        }

        Optional<ChecklistCompletion> existing = checklistCompletionRepository
                .findByChecklistItem_IdAndMonth(itemId, month);

        if (existing.isPresent()) {
            return new MarkCompletionResult(toCompletionResponseDTO(existing.get()), false);
        }

        ChecklistCompletion completion = new ChecklistCompletion();
        completion.setChecklistItem(item);
        completion.setMonth(month);
        completion.setCompletedAt(LocalDateTime.now());

        return new MarkCompletionResult(toCompletionResponseDTO(checklistCompletionRepository.save(completion)), true);
    }

    @Transactional
    public void unmarkComplete(Long itemId, String month) {
        validateMonth(month);
        UserAccount currentUser = getAuthenticatedUser();

        ChecklistItem item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Checklist item not found"));

        if (!item.getAccount().getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        if (Boolean.FALSE.equals(item.getActive())) {
            throw new RuntimeException("Checklist item not found");
        }

        checklistCompletionRepository
                .findByChecklistItem_IdAndMonth(itemId, month)
                .ifPresent(checklistCompletionRepository::delete);
    }

    public List<Long> getCompletedItemIds(Long accountId, String month) {
        validateMonth(month);
        UserAccount currentUser = getAuthenticatedUser();

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        return checklistCompletionRepository
                .findActiveByAccountIdAndMonth(accountId, month)
                .stream()
                .map(c -> c.getChecklistItem().getId())
                .toList();
    }
}
