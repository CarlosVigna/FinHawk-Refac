package com.carlos.finhawk_refac.repository;

import com.carlos.finhawk_refac.entity.Account;
import com.carlos.finhawk_refac.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByAccount(Account account);
}
