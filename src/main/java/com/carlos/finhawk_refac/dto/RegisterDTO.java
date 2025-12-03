package com.carlos.finhawk_refac.dto;

import com.carlos.finhawk_refac.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterDTO (@NotBlank @Email String email,@NotBlank String password,@NotBlank  UserRole role) {
}
