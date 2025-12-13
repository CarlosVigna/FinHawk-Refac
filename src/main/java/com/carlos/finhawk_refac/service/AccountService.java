package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.dto.request.AccountRequestDTO;
import com.carlos.finhawk_refac.dto.response.AccountResponseDTO;
import com.carlos.finhawk_refac.entity.Account;
import com.carlos.finhawk_refac.entity.UserAccount;
import com.carlos.finhawk_refac.repository.AccountRepository;
import com.carlos.finhawk_refac.repository.UserAccountRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserAccountRepository userAccountRepository;

    public AccountService(AccountRepository accountRepository, UserAccountRepository userAccountRepository) {
        this.accountRepository = accountRepository;
        this.userAccountRepository = userAccountRepository;
    }

    private UserAccount getAuthenticatedUser() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public AccountResponseDTO create(AccountRequestDTO dto) {
        UserAccount currentUser = getAuthenticatedUser();

        Account newAccount = new Account();
        newAccount.setName(dto.name());
        newAccount.setPhotoUrl(dto.photoUrl());
        newAccount.setUserAccount(currentUser);

        Account saved = accountRepository.save(newAccount);

        return new AccountResponseDTO(
                saved.getId(),
                saved.getName(),
                saved.getPhotoUrl()
        );
    }

    public AccountResponseDTO update(Long id, AccountRequestDTO dto) {
        UserAccount currentUser = getAuthenticatedUser();

        Account oldAccount = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!oldAccount.getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to update this account");
        }

        oldAccount.setName(dto.name());
        oldAccount.setPhotoUrl(dto.photoUrl());

        Account updated = accountRepository.save(oldAccount);

        return new AccountResponseDTO(
                updated.getId(),
                updated.getName(),
                updated.getPhotoUrl()
        );
    }

    public List<AccountResponseDTO> getAll() {
        UserAccount currentUser = getAuthenticatedUser();

        return accountRepository.findAllByUserAccount(currentUser)
                .stream().map(account -> new AccountResponseDTO(
                        account.getId(),
                        account.getName(),
                        account.getPhotoUrl()
                )).toList();
    }

    public AccountResponseDTO getById(Long id) {
        UserAccount currentUser = getAuthenticatedUser();

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        return new AccountResponseDTO(
                account.getId(),
                account.getName(),
                account.getPhotoUrl()
        );
    }

    public void delete(Long id) {
        UserAccount currentUser = getAuthenticatedUser();

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to delete this account");
        }

        accountRepository.delete(account);
    }
}