package com.carlos.finhawk_refac.repository;

import com.carlos.finhawk_refac.entity.ChecklistCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChecklistCompletionRepository extends JpaRepository<ChecklistCompletion, Long> {

    Optional<ChecklistCompletion> findByChecklistItem_IdAndMonth(Long checklistItemId, String month);

    @Query("SELECT c FROM ChecklistCompletion c WHERE c.checklistItem.account.id = :accountId AND c.month = :month AND c.checklistItem.active = true")
    List<ChecklistCompletion> findActiveByAccountIdAndMonth(@Param("accountId") Long accountId, @Param("month") String month);
}
