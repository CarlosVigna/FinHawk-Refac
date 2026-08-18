package com.carlos.finhawk_refac.repository;

import com.carlos.finhawk_refac.entity.DayTypeOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DayTypeOverrideRepository extends JpaRepository<DayTypeOverride, Long> {
    Optional<DayTypeOverride> findByAccount_IdAndDate(Long accountId, LocalDate date);
}
