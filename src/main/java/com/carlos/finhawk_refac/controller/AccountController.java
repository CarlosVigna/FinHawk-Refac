package com.carlos.finhawk_refac.controller;

import com.carlos.finhawk_refac.dto.request.AccountRequestDTO;
import com.carlos.finhawk_refac.dto.response.AccountResponseDTO;
import com.carlos.finhawk_refac.dto.response.UserAccountResponseDTO;
import com.carlos.finhawk_refac.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/account")
public class AccountController {

    public final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponseDTO> create (@RequestBody AccountRequestDTO dto){
        AccountResponseDTO newAccount = accountService.create(dto);
        return ResponseEntity.ok(newAccount);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponseDTO> update (@PathVariable Long id, @RequestBody AccountRequestDTO dto){
        AccountResponseDTO updated = accountService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    @GetMapping
    public ResponseEntity<List<AccountResponseDTO>> getAll (){
        List<AccountResponseDTO> accounts = accountService.getAll();
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponseDTO> getById(@PathVariable Long id){
        AccountResponseDTO account = accountService.getById(id);
        return ResponseEntity.ok(account);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        accountService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
