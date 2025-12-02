package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.dto.request.AccountRequestDTO;
import com.carlos.finhawk_refac.dto.response.AccountResponseDTO;
import com.carlos.finhawk_refac.entity.Account;
import com.carlos.finhawk_refac.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountResponseDTO create (AccountRequestDTO dto){
        Account newAccount = new Account();
        newAccount.setName(dto.name());
        newAccount.setPhotoUrl(dto.photoUrl());

        Account saved = accountRepository.save(newAccount);

        return new AccountResponseDTO(
        saved.getId(),
        saved.getName(),
        saved.getPhotoUrl()
        );

    }

    public AccountResponseDTO update (Long id, AccountRequestDTO dto){
        Account oldAccount = accountRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Account not found"));
        oldAccount.setName(dto.name());
        oldAccount.setPhotoUrl(dto.photoUrl());

        Account updated = accountRepository.save(oldAccount);

        return new AccountResponseDTO(
                updated.getId(),
                updated.getName(),
                updated.getPhotoUrl()
        );

    }

    public List<AccountResponseDTO> getAll(){
        return accountRepository.findAll()
                .stream().map(account -> new AccountResponseDTO(
                        account.getId(),
                        account.getName(),
                        account.getPhotoUrl()
                )).toList();
    }

    public AccountResponseDTO getById(Long id){
        Account account = accountRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Account not found"));

        return new AccountResponseDTO(
                account.getId(),
                account.getName(),
                account.getPhotoUrl()
        );
    }

    public void delete(Long id){
        Account account = accountRepository.findById(id).orElseThrow(() -> new RuntimeException("Account not found"));

        accountRepository.delete(account);
    }
}
