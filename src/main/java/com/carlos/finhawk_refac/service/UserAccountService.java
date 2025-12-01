package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.dto.request.UserAccountRequestDTO;
import com.carlos.finhawk_refac.dto.response.UserAccountResponseDTO;
import com.carlos.finhawk_refac.entity.UserAccount;
import com.carlos.finhawk_refac.repository.UserAccountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Service
public class UserAccountService {

    public final UserAccountRepository userAccountRepository;

    public UserAccountService(UserAccountRepository accountRepository) {
        this.userAccountRepository = accountRepository;
    }

    public ResponseEntity<UserAccountResponseDTO> create (@RequestBody UserAccountRequestDTO userDto){
        UserAccount newUser = new UserAccount();
        newUser.setName(userDto.name());
        newUser.setEmail(userDto.email());
        newUser.setPassword(userDto.password());

        UserAccount saved = userAccountRepository.save(newUser);

        return ResponseEntity.ok(
                new UserAccountResponseDTO(
                saved.getId(),
                saved.getName(),
                saved.getEmail()
                )
        );
    }

    public ResponseEntity<UserAccountResponseDTO> update (@PathVariable Long id, @RequestBody UserAccountRequestDTO userDto){
        UserAccount oldUser = userAccountRepository.findById(id).orElse(null);
        if (oldUser == null) {
            return ResponseEntity.notFound().build();
        }
        oldUser.setName(userDto.name());
        oldUser.setEmail(userDto.email());
        oldUser.setPassword(userDto.password());

        UserAccount updated = userAccountRepository.save(oldUser);

        UserAccountResponseDTO response = new UserAccountResponseDTO(
                updated.getId(),
                updated.getName(),
                updated.getEmail()
        );

        return ResponseEntity.ok(response);

    }
}
