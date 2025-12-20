package com.carlos.finhawk_refac.controller;


import com.carlos.finhawk_refac.dto.request.UserAccountRequestDTO;
import com.carlos.finhawk_refac.dto.response.UserAccountResponseDTO;
import com.carlos.finhawk_refac.service.UserAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserAccountController {

    public final UserAccountService userAccountService;

    public UserAccountController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }



    @PutMapping("/{id}")
    public ResponseEntity<UserAccountResponseDTO> update(@PathVariable Long id, @RequestBody UserAccountRequestDTO dto){
        UserAccountResponseDTO updated = userAccountService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    @GetMapping
    public ResponseEntity<List<UserAccountResponseDTO>> getAll() {
        List<UserAccountResponseDTO> users = userAccountService.getAll();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserAccountResponseDTO> getById(@PathVariable Long id){
        UserAccountResponseDTO user = userAccountService.getById(id);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete (@PathVariable Long id){
        userAccountService.delete(id);
        return ResponseEntity.noContent().build();

    }
}
