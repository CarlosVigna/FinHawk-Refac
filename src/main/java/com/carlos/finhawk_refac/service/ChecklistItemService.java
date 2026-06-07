package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.dto.request.ChecklistItemRequestDTO;
import com.carlos.finhawk_refac.dto.response.ChecklistItemResponseDTO;
import com.carlos.finhawk_refac.entity.Account;
import com.carlos.finhawk_refac.entity.ChecklistItem;
import com.carlos.finhawk_refac.entity.UserAccount;
import com.carlos.finhawk_refac.repository.AccountRepository;
import com.carlos.finhawk_refac.repository.ChecklistItemRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDate;
import java.math.BigDecimal;
import com.carlos.finhawk_refac.dto.response.ChecklistSuggestionDTO;
import com.carlos.finhawk_refac.entity.Bill;
import java.util.Comparator;

@Service
public class ChecklistItemService {

    private final ChecklistItemRepository checklistItemRepository;
    private final AccountRepository accountRepository;
    private final com.carlos.finhawk_refac.repository.BillRepository billRepository;

    public ChecklistItemService(ChecklistItemRepository checklistItemRepository,
                                AccountRepository accountRepository,
                                com.carlos.finhawk_refac.repository.BillRepository billRepository) {
        this.checklistItemRepository = checklistItemRepository;
        this.accountRepository = accountRepository;
        this.billRepository = billRepository;
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
                item.getAccount().getId()
        );
    }

    public ChecklistItemResponseDTO create(ChecklistItemRequestDTO dto) {
        UserAccount currentUser = getAuthenticatedUser();

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

        ChecklistItem saved = checklistItemRepository.save(item);
        return toResponseDTO(saved);
    }

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
        return toResponseDTO(updated);
    }

    public List<ChecklistItemResponseDTO> getAllByAccountId(Long accountId) {
        UserAccount currentUser = getAuthenticatedUser();

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        return checklistItemRepository.findAllByAccount_IdOrderByDueDayAsc(accountId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public void delete(Long id) {
        UserAccount currentUser = getAuthenticatedUser();

        ChecklistItem item = checklistItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Checklist item not found"));

        if (!item.getAccount().getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        checklistItemRepository.delete(item);
    }

    public ChecklistSuggestionDTO getSuggestion(Long checklistId) {
        UserAccount currentUser = getAuthenticatedUser();

        System.out.println("[ChecklistItemService] getSuggestion called for checklistId=" + checklistId + " by user id=" + currentUser.getId());

        ChecklistItem item = checklistItemRepository.findById(checklistId)
                .orElseThrow(() -> new RuntimeException("Checklist item not found"));

        System.out.println("[ChecklistItemService] checklist item accountId=" + item.getAccount().getId() + " item.account.userId=" + item.getAccount().getUserAccount().getId());

        if (!item.getAccount().getUserAccount().getId().equals(currentUser.getId())) {
            System.out.println("[ChecklistItemService] Access denied: checklist owner=" + item.getAccount().getUserAccount().getId() + " requester=" + currentUser.getId());
            throw new RuntimeException("Access denied");
        }

        List<Bill> bills = billRepository.findAllByAccount_Id(item.getAccount().getId());

        // Find last bill whose description contains checklist description (case-insensitive)
        return bills.stream()
                .filter(b -> b.getDescription() != null && b.getDescription().toLowerCase().contains(item.getDescription().toLowerCase()))
                .max(Comparator.comparing(Bill::getMaturity))
                .map(b -> new ChecklistSuggestionDTO(
                        b.getInstallmentAmount(),
                        b.getCategory() != null ? b.getCategory().getId() : null,
                        b.getMaturity(),
                        b.getDescription()
                ))
                .orElse(new ChecklistSuggestionDTO(null, null, null, null));
    }
}