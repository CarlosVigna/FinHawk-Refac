package com.carlos.finhawk_refac.repository;

import com.carlos.finhawk_refac.entity.Account;
import com.carlos.finhawk_refac.entity.Bill;
import com.carlos.finhawk_refac.enums.StatusBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    List<Bill> findAllByAccount_Id(Long accountId);

    List<Bill> findAllByStatus(StatusBill status);

    List<Bill> findAllByAccountAndStatus(Account account, StatusBill status);

    List<Bill> findAllByAccount_IdAndMaturityBetween(Long accountId, LocalDate start, LocalDate end);

    boolean existsByAccount_IdAndDescriptionAndMaturityAndInstallmentAmount(
            Long accountId, String description, LocalDate maturity, BigDecimal installmentAmount);

    long countByAccount_UserAccount_Id(Long userId);

    // Usados pelos schedulers de notificacao via WhatsApp -- varrem todas as
    // contas porque as notificacoes vao pra um unico grupo compartilhado da familia.
    List<Bill> findAllByMaturityAndStatus(LocalDate maturity, StatusBill status);

    List<Bill> findAllByMaturityBetweenAndStatus(LocalDate start, LocalDate end, StatusBill status);

    // Todos os status (nao so PENDING) -- usados pelos resumos de hoje/semana
    // (Parte 3), que mostram tanto o que falta quanto o que ja foi pago/
    // recebido, em vez de so o que esta pendente.
    List<Bill> findAllByMaturity(LocalDate maturity);

    List<Bill> findAllByMaturityBetween(LocalDate start, LocalDate end);
}