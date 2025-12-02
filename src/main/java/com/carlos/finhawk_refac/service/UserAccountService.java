package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.dto.request.UserAccountRequestDTO;
import com.carlos.finhawk_refac.dto.response.UserAccountResponseDTO;
import com.carlos.finhawk_refac.entity.UserAccount;
import com.carlos.finhawk_refac.repository.UserAccountRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;

    public UserAccountService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }


    public UserAccountResponseDTO create(UserAccountRequestDTO userDto) {
        UserAccount newUser = new UserAccount();
        newUser.setName(userDto.name());
        newUser.setEmail(userDto.email());
        newUser.setPassword(userDto.password());

        UserAccount saved = userAccountRepository.save(newUser);

        return new UserAccountResponseDTO(
                saved.getId(),
                saved.getName(),
                saved.getEmail()
        );
    }


    public UserAccountResponseDTO update(Long id, UserAccountRequestDTO userDto) {
        UserAccount oldUser = userAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        oldUser.setName(userDto.name());
        oldUser.setEmail(userDto.email());
        oldUser.setPassword(userDto.password());

        UserAccount updated = userAccountRepository.save(oldUser);

        return new UserAccountResponseDTO(
                updated.getId(),
                updated.getName(),
                updated.getEmail()
        );
    }


    public List<UserAccountResponseDTO> getAll() {
        return userAccountRepository.findAll()
                .stream()
                .map(user -> new UserAccountResponseDTO(
                        user.getId(),
                        user.getName(),
                        user.getEmail()
                ))
                .toList();
    }

    public UserAccountResponseDTO getById(Long id) {
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new UserAccountResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }
    public void delete(Long id){
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        userAccountRepository.delete(user);
    }
}
