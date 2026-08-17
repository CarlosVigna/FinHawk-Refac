package com.carlos.finhawk_refac.repository;

import com.carlos.finhawk_refac.entity.AgendaEventCompletion;
import com.carlos.finhawk_refac.enums.AgendaCompletionStatus;
import com.carlos.finhawk_refac.enums.AgendaEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgendaEventCompletionRepository extends JpaRepository<AgendaEventCompletion, Long> {

    Optional<AgendaEventCompletion> findByAgendaEvent_IdAndEventDate(Long agendaEventId, LocalDate eventDate);

    List<AgendaEventCompletion> findAllByAgendaEvent_Account_IdAndEventDate(Long accountId, LocalDate eventDate);

    // Resumo consolidado de habito concluido (AgendaNotificationScheduler.habitCompletionDigest).
    List<AgendaEventCompletion> findAllByStatusAndNotifiedAtIsNullAndAgendaEvent_Type(
            AgendaCompletionStatus status, AgendaEventType type);
}
