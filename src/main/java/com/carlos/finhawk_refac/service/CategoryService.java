package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.dto.request.CategoryRequestDTO;
import com.carlos.finhawk_refac.dto.response.CategoryResponseDTO;
import com.carlos.finhawk_refac.entity.Account;
import com.carlos.finhawk_refac.entity.Category;
import com.carlos.finhawk_refac.entity.UserAccount;
import com.carlos.finhawk_refac.enums.CategoryType;
import com.carlos.finhawk_refac.repository.AccountRepository;
import com.carlos.finhawk_refac.repository.CategoryRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final AuditLogService auditLogService;

    public CategoryService(CategoryRepository categoryRepository, AccountRepository accountRepository,
                            AuditLogService auditLogService) {
        this.categoryRepository = categoryRepository;
        this.accountRepository = accountRepository;
        this.auditLogService = auditLogService;
    }

    private UserAccount getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserAccount)) {
            throw new RuntimeException("Unauthenticated user");
        }

        return (UserAccount) authentication.getPrincipal();
    }

    @Transactional
    public CategoryResponseDTO create(CategoryRequestDTO dto) {
        UserAccount currentUser = getAuthenticatedUser();

        if (dto.name() == null || dto.name().isBlank()) {
            throw new RuntimeException("O nome da categoria é obrigatório.");
        }

        if (dto.type() == null || dto.type().isBlank()) {
            throw new RuntimeException("O tipo da categoria é obrigatório.");
        }

        if (dto.accountId() == null) {
            throw new RuntimeException("Account not found");
        }

        Account account = accountRepository.findById(dto.accountId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to create categories in this account");
        }

        Category newCategory = new Category();
        newCategory.setName(dto.name());
        newCategory.setType(CategoryType.valueOf(dto.type().toUpperCase()));
        newCategory.setAccount(account);

        Category saved = categoryRepository.save(newCategory);

        auditLogService.record(currentUser, AuditLogService.CREATE, "Category", saved.getId(), saved.getName());

        return new CategoryResponseDTO(
                saved.getId(),
                saved.getName(),
                saved.getType().name()
        );
    }

    @Transactional
    public CategoryResponseDTO update(Long id, CategoryRequestDTO dto) {
        UserAccount currentUser = getAuthenticatedUser();

        Category oldCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!oldCategory.getAccount().getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to update this category");
        }

        if (oldCategory.getDeletedAt() != null) {
            throw new RuntimeException("Category not found or archived");
        }

        if (dto.accountId() != null && !dto.accountId().equals(oldCategory.getAccount().getId())) {
            Account newAccount = accountRepository.findById(dto.accountId())
                    .orElseThrow(() -> new RuntimeException("Account not found"));

            if (!newAccount.getUserAccount().getId().equals(currentUser.getId())) {
                throw new RuntimeException("You are not allowed to move category to this account");
            }

            oldCategory.setAccount(newAccount);
        }

        if (dto.name() != null && !dto.name().isBlank()) {
            oldCategory.setName(dto.name());
        }

        if (dto.type() != null && !dto.type().isBlank()) {
            oldCategory.setType(CategoryType.valueOf(dto.type().toUpperCase()));
        }

        Category updated = categoryRepository.save(oldCategory);

        auditLogService.record(currentUser, AuditLogService.UPDATE, "Category", updated.getId(), updated.getName());

        return new CategoryResponseDTO(
                updated.getId(),
                updated.getName(),
                updated.getType().name()
        );
    }

    public CategoryResponseDTO getById(Long id) {
        UserAccount currentUser = getAuthenticatedUser();

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!category.getAccount().getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        if (category.getDeletedAt() != null) {
            throw new RuntimeException("Category not found");
        }

        return new CategoryResponseDTO(
                category.getId(),
                category.getName(),
                category.getType().name()
        );
    }

    @Transactional
    public void delete(Long id) {
        UserAccount currentUser = getAuthenticatedUser();

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!category.getAccount().getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to delete this category");
        }

        category.setDeletedAt(LocalDateTime.now());
        categoryRepository.save(category);

        auditLogService.record(currentUser, AuditLogService.DELETE, "Category", category.getId(), category.getName());
    }

    public List<CategoryResponseDTO> getAllByAccountId(Long accountId) {
        UserAccount currentUser = getAuthenticatedUser();

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        return categoryRepository.findAllByAccountAndDeletedAtIsNull(account).stream()
                .map(category -> new CategoryResponseDTO(
                        category.getId(),
                        category.getName(),
                        category.getType().name()
                ))
                .toList();
    }
}
