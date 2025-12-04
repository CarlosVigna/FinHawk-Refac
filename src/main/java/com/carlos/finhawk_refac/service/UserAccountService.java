package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.dto.request.UserAccountRequestDTO;
import com.carlos.finhawk_refac.dto.response.UserAccountResponseDTO;
import com.carlos.finhawk_refac.entity.UserAccount;
import com.carlos.finhawk_refac.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Service
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }


//    public UserAccountResponseDTO create(UserAccountRequestDTO userDto) {
//        UserAccount newUser = new UserAccount();
//        newUser.setName(userDto.name());
//        newUser.setEmail(userDto.email());
//        newUser.setPassword(userDto.password());
//
//        UserAccount saved = userAccountRepository.save(newUser);
//
//        return new UserAccountResponseDTO(
//                saved.getId(),
//                saved.getName(),
//                saved.getEmail()
//        );
//    }


    public UserAccountResponseDTO update(Long id, UserAccountRequestDTO userDto) {
        UserAccount oldUser = userAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if(userDto.name() != null) oldUser.setName(userDto.name());
        if(userDto.email() != null) oldUser.setEmail(userDto.email());

        if(userDto.password() != null && !userDto.password().isBlank()) {
            oldUser.setPassword(passwordEncoder.encode(userDto.password()));
        }

        if(userDto.role() != null) oldUser.setRole(userDto.role());

        UserAccount updated = userAccountRepository.save(oldUser);

        return new UserAccountResponseDTO(
                updated.getId(),
                updated.getName(),
                updated.getEmail(),
                updated.getRole().name()
        );
    }


    public List<UserAccountResponseDTO> getAll() {
        return userAccountRepository.findAll()
                .stream()
                .map(user -> new UserAccountResponseDTO(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole().name()
                ))
                .toList();
    }

    public UserAccountResponseDTO getById(Long id) {
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new UserAccountResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()

        );
    }
    public void delete(Long id){
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        userAccountRepository.delete(user);
    }
}
