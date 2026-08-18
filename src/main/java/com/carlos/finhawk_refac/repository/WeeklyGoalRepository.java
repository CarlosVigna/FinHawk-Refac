package com.carlos.finhawk_refac.repository;

import com.carlos.finhawk_refac.entity.WeeklyGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WeeklyGoalRepository extends JpaRepository<WeeklyGoal, Long> {

    List<WeeklyGoal> findAllByAccount_IdAndWeekStartDate(Long accountId, LocalDate weekStartDate);

    // Usado pelo WeeklyGoalScheduler (rollover semanal) e pelo resumo das
    // 07:30 (metas ainda nao concluidas).
    List<WeeklyGoal> findAllByWeekStartDateAndCompletedFalse(LocalDate weekStartDate);

    // Resumos de hoje/semana (Parte 3) -- todas as metas da semana atual
    // (feitas e pendentes), pra mostrar nas duas secoes.
    List<WeeklyGoal> findAllByWeekStartDate(LocalDate weekStartDate);
}
