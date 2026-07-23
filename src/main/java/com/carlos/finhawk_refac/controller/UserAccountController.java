package com.carlos.finhawk_refac.controller;

import com.carlos.finhawk_refac.dto.request.UserAccountRequestDTO;
import com.carlos.finhawk_refac.dto.request.UserDataImportDTO;
import com.carlos.finhawk_refac.dto.response.UserAccountResponseDTO;
import com.carlos.finhawk_refac.dto.response.UserDataExportDTO;
import com.carlos.finhawk_refac.entity.UserAccount;
import com.carlos.finhawk_refac.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserAccountController {

    public final UserAccountService userAccountService;

    public UserAccountController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserAccountResponseDTO> getMe(@AuthenticationPrincipal UserAccount userAccount) {
        return ResponseEntity.ok(new UserAccountResponseDTO(
                userAccount.getId(),
                userAccount.getName(),
                userAccount.getEmail(),
                userAccount.getRole().name()
        ));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal UserAccount userAccount) {
        userAccountService.deleteCurrentUser(userAccount);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserAccountResponseDTO> update(@PathVariable Long id, @Valid @RequestBody UserAccountRequestDTO dto) {
        UserAccountResponseDTO updated = userAccountService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/export")
    public ResponseEntity<UserDataExportDTO> export() {
        return ResponseEntity.ok(userAccountService.exportUserData());
    }

    @PostMapping("/import")
    public ResponseEntity<Void> importData(@RequestBody UserDataImportDTO dto) {
        userAccountService.importUserData(dto);
        return ResponseEntity.ok().build();
    }
}
