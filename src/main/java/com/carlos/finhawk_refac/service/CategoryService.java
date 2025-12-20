package com.carlos.finhawk_refac.service;


import com.carlos.finhawk_refac.dto.request.CategoryRequestDTO;
import com.carlos.finhawk_refac.dto.response.CategoryResponseDTO;
import com.carlos.finhawk_refac.entity.Account;
import com.carlos.finhawk_refac.entity.Category;
import com.carlos.finhawk_refac.entity.UserAccount;
import com.carlos.finhawk_refac.repository.AccountRepository;
import com.carlos.finhawk_refac.repository.CategoryRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;

    public CategoryService(CategoryRepository categoryRepository, AccountRepository accountRepository) {
        this.categoryRepository = categoryRepository;
        this.accountRepository = accountRepository;
    }

    private UserAccount getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserAccount)) {
            throw new RuntimeException("Unauthenticated user");
        }

        return (UserAccount) authentication.getPrincipal();
    }

    public CategoryResponseDTO create(CategoryRequestDTO dto) {
        UserAccount currentUser = getAuthenticatedUser();

        Account account = accountRepository.findById(dto.accountId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to create categories in this account");
        }

        Category newCategory = new Category();
        newCategory.setName(dto.name());
        newCategory.setType(dto.type());
        newCategory.setAccount(account);

        Category saved = categoryRepository.save(newCategory);

        return new CategoryResponseDTO(
                saved.getId(),
                saved.getName(),
                saved.getType()
        );
    }

    public CategoryResponseDTO update(Long id, CategoryRequestDTO dto) {
        UserAccount currentUser = getAuthenticatedUser();

        Category oldCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!oldCategory.getAccount().getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to update this category");
        }

        if (dto.accountId() != null && !dto.accountId().equals(oldCategory.getAccount().getId())) {
            Account newAccount = accountRepository.findById(dto.accountId())
                    .orElseThrow(() -> new RuntimeException("Account not found"));

            if (!newAccount.getUserAccount().getId().equals(currentUser.getId())) {
                throw new RuntimeException("You are not allowed to move category to this account");
            }

            oldCategory.setAccount(newAccount);
        }

        oldCategory.setName(dto.name());
        oldCategory.setType(dto.type());

        Category updated = categoryRepository.save(oldCategory);

        return new CategoryResponseDTO(
                updated.getId(),
                updated.getName(),
                updated.getType()
        );
    }

    public List<CategoryResponseDTO> getAll() {
        UserAccount currentUser = getAuthenticatedUser();

        List<Account> userAccounts = accountRepository.findAllByUserAccount(currentUser);

        return userAccounts.stream()
                .flatMap(account -> categoryRepository.findAllByAccount(account).stream())
                .map(category -> new CategoryResponseDTO(
                        category.getId(),
                        category.getName(),
                        category.getType()
                ))
                .toList();
    }

    public CategoryResponseDTO getById(Long id) {
        UserAccount currentUser = getAuthenticatedUser();

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!category.getAccount().getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        return new CategoryResponseDTO(
                category.getId(),
                category.getName(),
                category.getType()
        );
    }

    public void delete(Long id) {
        UserAccount currentUser = getAuthenticatedUser();

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!category.getAccount().getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to delete this category");
        }

        categoryRepository.delete(category);
    }

    public List<CategoryResponseDTO> getAllByAccountId(Long accountId) {
        UserAccount currentUser = getAuthenticatedUser();

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        return categoryRepository.findAllByAccount(account).stream()
                .map(category -> new CategoryResponseDTO(
                        category.getId(),
                        category.getName(),
                        category.getType()
                ))
                .toList();
    }


}
