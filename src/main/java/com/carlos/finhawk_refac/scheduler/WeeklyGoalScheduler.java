package com.carlos.finhawk_refac.scheduler;

import com.carlos.finhawk_refac.entity.WeeklyGoal;
import com.carlos.finhawk_refac.repository.WeeklyGoalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// So mexe em estado (recria a meta nao concluida na semana nova) -- nao
// manda WhatsApp, por isso fica separado de AgendaNotificationScheduler.
@Component
public class WeeklyGoalScheduler {

    private static final Logger log = LoggerFactory.getLogger(WeeklyGoalScheduler.class);
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private final WeeklyGoalRepository weeklyGoalRepository;

    public WeeklyGoalScheduler(WeeklyGoalRepository weeklyGoalRepository) {
        this.weeklyGoalRepository = weeklyGoalRepository;
    }

    // Toda segunda, logo depois da meia-noite: meta da semana passada que
    // ficou incompleta ganha uma copia nova pra semana atual. A antiga fica
    // de historico (so nao aparece mais em "semana atual").
    @Scheduled(cron = "0 10 0 * * MON", zone = "America/Sao_Paulo")
    @Transactional
    public void rolloverIncompleteGoals() {
        try {
            LocalDate thisMonday = LocalDate.now(ZONE).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate lastMonday = thisMonday.minusWeeks(1);

            List<WeeklyGoal> incomplete = weeklyGoalRepository.findAllByWeekStartDateAndCompletedFalse(lastMonday);
            if (incomplete.isEmpty()) {
                return;
            }

            // Protege contra reenvio duplicado se o scheduler rodar de novo
            // na mesma semana (ex: restart do servico) -- ja existe uma meta
            // com o mesmo titulo criada pra essa conta nesta semana, pula.
            Set<String> alreadyRolled = incomplete.stream()
                    .map(g -> g.getAccount().getId())
                    .distinct()
                    .flatMap(accountId -> weeklyGoalRepository.findAllByAccount_IdAndWeekStartDate(accountId, thisMonday).stream())
                    .map(g -> g.getAccount().getId() + "|" + g.getTitle())
                    .collect(Collectors.toSet());

            List<WeeklyGoal> rolled = incomplete.stream()
                    .filter(old -> !alreadyRolled.contains(old.getAccount().getId() + "|" + old.getTitle()))
                    .map(old -> {
                        WeeklyGoal fresh = new WeeklyGoal();
                        fresh.setTitle(old.getTitle());
                        fresh.setAccount(old.getAccount());
                        fresh.setWeekStartDate(thisMonday);
                        fresh.setCompleted(false);
                        return fresh;
                    })
                    .toList();

            weeklyGoalRepository.saveAll(rolled);
        } catch (Exception e) {
            log.error("Falha no job de rollover semanal de metas: {}", e.getMessage(), e);
        }
    }
}
