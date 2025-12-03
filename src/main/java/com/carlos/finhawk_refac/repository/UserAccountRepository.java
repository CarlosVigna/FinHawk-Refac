package com.carlos.finhawk_refac.repository;

import com.carlos.finhawk_refac.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    UserDetails findByLogin (String login);
}
