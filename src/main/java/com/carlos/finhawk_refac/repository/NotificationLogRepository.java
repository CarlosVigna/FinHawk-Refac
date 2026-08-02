package com.carlos.finhawk_refac.repository;

import com.carlos.finhawk_refac.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    boolean existsByJobKeyAndReferenceDate(String jobKey, LocalDate referenceDate);
}
