package com.carlos.finhawk_refac.repository;

import com.carlos.finhawk_refac.entity.PasswordResetToken;
import com.carlos.finhawk_refac.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    List<PasswordResetToken> findByUserAccountAndUsedFalse(UserAccount userAccount);
}
