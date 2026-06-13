package com.carlos.finhawk_refac.repository;

import com.carlos.finhawk_refac.entity.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {
    List<ChecklistItem> findAllByAccount_IdAndActiveTrueOrderByDueDayAsc(Long accountId);

    List<ChecklistItem> findAllByAccount_Id(Long accountId);

    boolean existsByAccount_IdAndDescriptionAndActiveTrue(Long accountId, String description);
}
