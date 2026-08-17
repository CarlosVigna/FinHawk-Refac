package com.carlos.finhawk_refac.scheduler;

import com.carlos.finhawk_refac.entity.AgendaEvent;
import com.carlos.finhawk_refac.enums.AgendaCompletionStatus;
import com.carlos.finhawk_refac.enums.AgendaEventType;
import com.carlos.finhawk_refac.repository.AgendaEventCompletionRepository;
import com.carlos.finhawk_refac.repository.AgendaEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

// So mexe em estado (flag pendingRollover em AgendaEvent) -- nao manda
// WhatsApp nenhum, por isso fica separado de AgendaNotificationScheduler
// (que fica focado em jobs que de fato notificam). O check-in em si
// acontece dentro do app (ver AgendaEventController/AgendaEventService),
// nao aqui.
@Component
public class AgendaRolloverScheduler {

    private static final Logger log = LoggerFactory.getLogger(AgendaRolloverScheduler.class);
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private final AgendaEventRepository agendaEventRepository;
    private final AgendaEventCompletionRepository agendaEventCompletionRepository;

    public AgendaRolloverScheduler(AgendaEventRepository agendaEventRepository,
                                    AgendaEventCompletionRepository agendaEventCompletionRepository) {
        this.agendaEventRepository = agendaEventRepository;
        this.agendaEventCompletionRepository = agendaEventCompletionRepository;
    }

    // Logo depois da meia-noite: evento pontual de ontem, ativo, sem
    // completion DONE pra ontem -> fica marcado pendente de confirmacao.
    @Scheduled(cron = "0 5 0 * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void flagPendingRollovers() {
        try {
            LocalDate yesterday = LocalDate.now(ZONE).minusDays(1);

            List<AgendaEvent> yesterdayEvents = agendaEventRepository
                    .findAllByTypeAndActiveTrueAndDeletedAtIsNullAndEventDateTimeBetween(
                            AgendaEventType.ONE_TIME, yesterday.atStartOfDay(), yesterday.plusDays(1).atStartOfDay());

            List<AgendaEvent> toFlag = yesterdayEvents.stream()
                    .filter(event -> !wasCompletedDone(event, yesterday))
                    .toList();

            toFlag.forEach(event -> event.setPendingRollover(true));
            agendaEventRepository.saveAll(toFlag);
        } catch (Exception e) {
            log.error("Falha no job de rollover de eventos: {}", e.getMessage(), e);
        }
    }

    private boolean wasCompletedDone(AgendaEvent event, LocalDate date) {
        return agendaEventCompletionRepository.findByAgendaEvent_IdAndEventDate(event.getId(), date)
                .map(c -> c.getStatus() == AgendaCompletionStatus.DONE)
                .orElse(false);
    }
}
