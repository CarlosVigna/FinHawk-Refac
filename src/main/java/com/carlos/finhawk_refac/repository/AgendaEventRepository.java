package com.carlos.finhawk_refac.repository;

import com.carlos.finhawk_refac.entity.AgendaEvent;
import com.carlos.finhawk_refac.enums.AgendaEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AgendaEventRepository extends JpaRepository<AgendaEvent, Long> {

    // Ordenado por horario crescente na propria query -- eventDateTime pra
    // ONE_TIME, timeOfDay pra HABIT (o outro campo fica nulo conforme o tipo,
    // entao o segundo criterio so entra em jogo quando o primeiro empata,
    // que e sempre o caso dentro de uma unica lista filtrada por tipo).
    // Ordenar aqui, na fonte, evita depender de cada tela do frontend
    // reordenar por conta propria.
    List<AgendaEvent> findAllByAccount_IdAndDeletedAtIsNullOrderByEventDateTimeAscTimeOfDayAsc(Long accountId);

    List<AgendaEvent> findAllByAccount_IdAndTypeAndDeletedAtIsNullOrderByEventDateTimeAscTimeOfDayAsc(
            Long accountId, AgendaEventType type);

    // Lembrete "1h antes": eventos pontuais ativos, ainda nao notificados,
    // cujo horario cai dentro da janela verificada pelo job.
    List<AgendaEvent> findAllByTypeAndActiveTrueAndDeletedAtIsNullAndReminderSentAtIsNullAndEventDateTimeBetween(
            AgendaEventType type, LocalDateTime start, LocalDateTime end);

    // Resumo noturno: eventos pontuais ativos programados pro dia seguinte.
    List<AgendaEvent> findAllByTypeAndActiveTrueAndDeletedAtIsNullAndEventDateTimeBetween(
            AgendaEventType type, LocalDateTime start, LocalDateTime end);

    // Habitos ativos, usados pelo resumo noturno pra descobrir quais tocam amanha.
    List<AgendaEvent> findAllByTypeAndActiveTrueAndDeletedAtIsNull(AgendaEventType type);

    // Check-in de rollover: eventos ONE_TIME pendentes de confirmacao ("voce
    // ja fez isso?") de uma conta.
    List<AgendaEvent> findAllByAccount_IdAndTypeAndPendingRolloverTrueAndDeletedAtIsNull(
            Long accountId, AgendaEventType type);
}
