package com.carlos.finhawk_refac.repository;

import com.carlos.finhawk_refac.entity.Account;
import com.carlos.finhawk_refac.entity.Bill;
import com.carlos.finhawk_refac.enums.StatusBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {
    List<Bill> findAllByAccount(Account account);

    List<Bill> findAllByStatus(StatusBill status);

    List<Bill> findAllByAccountAndStatus(Account account, StatusBill status);
}