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

@Service
public class ChecklistItemService {

    private final ChecklistItemRepository checklistItemRepository;
    private final AccountRepository accountRepository;

    public ChecklistItemService(ChecklistItemRepository checklistItemRepository,
                                AccountRepository accountRepository) {
        this.checklistItemRepository = checklistItemRepository;
        this.accountRepository = accountRepository;
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
}