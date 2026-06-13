package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.entity.PasswordResetToken;
import com.carlos.finhawk_refac.entity.UserAccount;
import com.carlos.finhawk_refac.repository.PasswordResetTokenRepository;
import com.carlos.finhawk_refac.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserAccountService userAccountService;
    private final EmailService emailService;

    public PasswordResetService(
            UserAccountRepository userAccountRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            UserAccountService userAccountService,
            EmailService emailService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.userAccountService = userAccountService;
        this.emailService = emailService;
    }

    @Transactional
    public void requestReset(String email) {
        Optional<UserAccount> userOpt = userAccountRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // AO-01: silently ignore unknown e-mail to prevent user enumeration
            return;
        }

        UserAccount user = userOpt.get();

        // AO-02: invalidate all existing active tokens before creating a new one
        List<PasswordResetToken> activeTokens =
                passwordResetTokenRepository.findByUserAccountAndUsedFalse(user);
        for (PasswordResetToken existing : activeTokens) {
            existing.setUsed(true);
        }
        if (!activeTokens.isEmpty()) {
            passwordResetTokenRepository.saveAll(activeTokens);
        }

        String tokenValue = UUID.randomUUID().toString();
        Instant now = Instant.now();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(tokenValue);
        resetToken.setUserAccount(user);
        resetToken.setExpiresAt(now.plus(30, ChronoUnit.MINUTES));
        resetToken.setUsed(false);
        resetToken.setCreatedAt(now);

        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user.getEmail(), tokenValue);
    }

    @Transactional  // AO-03: password update + token invalidation are atomic
    public void resetPassword(String tokenValue, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new RuntimeException("Token inválido ou expirado."));

        if (resetToken.isUsed()) {
            throw new RuntimeException("Token inválido ou expirado.");
        }

        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Token inválido ou expirado.");
        }

        userAccountService.updatePassword(resetToken.getUserAccount().getId(), newPassword);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }
}
