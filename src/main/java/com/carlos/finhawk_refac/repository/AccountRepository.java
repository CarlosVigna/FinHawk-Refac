package com.carlos.finhawk_refac.repository;

import com.carlos.finhawk_refac.entity.Account;
import com.carlos.finhawk_refac.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    // Busca todas as contas que pertencem a um usuário específico
    List<Account> findAllByUserAccount(UserAccount userAccount);
}