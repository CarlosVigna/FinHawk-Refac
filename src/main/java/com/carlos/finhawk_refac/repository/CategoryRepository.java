package com.carlos.finhawk_refac.repository;

import com.carlos.finhawk_refac.entity.Account;
import com.carlos.finhawk_refac.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByAccountAndDeletedAtIsNull(Account account);
    Optional<Category> findByAccountAndNameAndDeletedAtIsNull(Account account, String name);
}
