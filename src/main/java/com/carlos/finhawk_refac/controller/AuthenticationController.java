package com.carlos.finhawk_refac.controller;

import com.carlos.finhawk_refac.config.security.TokenService;
import com.carlos.finhawk_refac.dto.AuthenticationDTO;
import com.carlos.finhawk_refac.dto.ForgotPasswordDTO;
import com.carlos.finhawk_refac.dto.RegisterDTO;
import com.carlos.finhawk_refac.dto.ResetPasswordDTO;
import com.carlos.finhawk_refac.dto.response.LoginResponseDTO;
import com.carlos.finhawk_refac.entity.UserAccount;
import com.carlos.finhawk_refac.enums.UserRole;
import com.carlos.finhawk_refac.repository.UserAccountRepository;
import com.carlos.finhawk_refac.service.EmailService;
import com.carlos.finhawk_refac.service.LoginAttemptService;
import com.carlos.finhawk_refac.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final UserAccountRepository userAccountRepository;
    private final TokenService tokenService;
    private final PasswordResetService passwordResetService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final LoginAttemptService loginAttemptService;

    public AuthenticationController(AuthenticationManager authenticationManager,
                                    UserAccountRepository userAccountRepository,
                                    TokenService tokenService,
                                    PasswordResetService passwordResetService,
                                    PasswordEncoder passwordEncoder,
                                    EmailService emailService,
                                    LoginAttemptService loginAttemptService) {
        this.authenticationManager = authenticationManager;
        this.userAccountRepository = userAccountRepository;
        this.tokenService = tokenService;
        this.passwordResetService = passwordResetService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.loginAttemptService = loginAttemptService;
    }

    @PostMapping("/login")
    public ResponseEntity login(@RequestBody @Valid AuthenticationDTO data, HttpServletRequest request) {
        String ip = clientIp(request);
        loginAttemptService.checkAllowed(ip, data.email());

        try {
            var usernamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.password());
            var auth = this.authenticationManager.authenticate(usernamePassword);
            UserAccount user = (UserAccount) auth.getPrincipal();
            loginAttemptService.registerSuccess(ip, data.email());
            String token = tokenService.generateToken(user);
            String refreshToken = tokenService.generateRefreshToken(user);
            return ResponseEntity.ok(new LoginResponseDTO(token, refreshToken));
        } catch (AuthenticationException ex) {
            loginAttemptService.registerFailure(ip, data.email());
            throw ex;
        }
    }

    // Railway/Vercel ficam atras de proxy -- o IP real do cliente vem no
    // X-Forwarded-For, nao em request.getRemoteAddr() (que seria o proxy).
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/refresh")
    public ResponseEntity refresh(@RequestBody java.util.Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body("Missing refreshToken");
        }
        String email = tokenService.validateRefreshToken(refreshToken);
        if (email == null) {
            return ResponseEntity.status(401).body("Invalid or expired refresh token");
        }
        UserAccount user = (UserAccount) userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String newToken = tokenService.generateToken(user);
        String newRefreshToken = tokenService.generateRefreshToken(user);
        return ResponseEntity.ok(new LoginResponseDTO(newToken, newRefreshToken));
    }

    @PostMapping("/register")
    public ResponseEntity register(@RequestBody @Valid RegisterDTO data) {
        if (this.userAccountRepository.findByEmail(data.email()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already registered");
        }

        String encryptedPassword = passwordEncoder.encode(data.password());

        UserAccount newUser = new UserAccount(
                data.name(),
                data.email(),
                encryptedPassword,
                UserRole.VIEWER
        );

        this.userAccountRepository.save(newUser);

        emailService.sendWelcomeEmail(newUser.getEmail(), newUser.getName());

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity forgotPassword(@RequestBody @Valid ForgotPasswordDTO data) {
        passwordResetService.requestReset(data.email());
        return ResponseEntity.ok().build(); // AO-01: always 200, never reveal e-mail existence
    }

    @PostMapping("/reset-password")
    public ResponseEntity resetPassword(@RequestBody @Valid ResetPasswordDTO data) {
        passwordResetService.resetPassword(data.token(), data.newPassword());
        return ResponseEntity.ok().build();
    }
}
