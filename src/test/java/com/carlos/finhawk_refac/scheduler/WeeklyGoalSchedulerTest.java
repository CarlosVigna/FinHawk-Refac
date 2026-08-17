package com.carlos.finhawk_refac.scheduler;

import com.carlos.finhawk_refac.entity.Account;
import com.carlos.finhawk_refac.entity.WeeklyGoal;
import com.carlos.finhawk_refac.repository.WeeklyGoalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Cobre a logica de rollover semanal de metas (Parte 6) -- em especial a
// guarda contra duplicar a meta nova se o job rodar de novo na mesma semana
// (ex: restart do servico), que nao tem nenhum outro mecanismo de dedup
// (diferente dos jobs de notificacao, que usam NotificationLog -- aqui nao
// ha notificacao nenhuma, so mutacao de estado).
@ExtendWith(MockitoExtension.class)
class WeeklyGoalSchedulerTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    @Mock
    private WeeklyGoalRepository weeklyGoalRepository;

    private WeeklyGoalScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new WeeklyGoalScheduler(weeklyGoalRepository);
    }

    private static LocalDate thisMonday() {
        return LocalDate.now(ZONE).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static LocalDate lastMonday() {
        return thisMonday().minusWeeks(1);
    }

    private Account account(Long id) {
        Account a = new Account();
        a.setId(id);
        return a;
    }

    private WeeklyGoal goal(Account account, String title, LocalDate weekStart, boolean completed) {
        WeeklyGoal g = new WeeklyGoal();
        g.setAccount(account);
        g.setTitle(title);
        g.setWeekStartDate(weekStart);
        g.setCompleted(completed);
        return g;
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<WeeklyGoal>> savedListCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }

    @Test
    void metaIncompleta_ganhaCopiaNovaParaSemanaAtual() {
        Account account = account(1L);
        WeeklyGoal incomplete = goal(account, "Treinar 3x", lastMonday(), false);

        when(weeklyGoalRepository.findAllByWeekStartDateAndCompletedFalse(lastMonday()))
                .thenReturn(List.of(incomplete));
        when(weeklyGoalRepository.findAllByAccount_IdAndWeekStartDate(1L, thisMonday()))
                .thenReturn(List.of());

        scheduler.rolloverIncompleteGoals();

        ArgumentCaptor<List<WeeklyGoal>> captor = savedListCaptor();
        verify(weeklyGoalRepository).saveAll(captor.capture());

        List<WeeklyGoal> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getTitle()).isEqualTo("Treinar 3x");
        assertThat(saved.get(0).getWeekStartDate()).isEqualTo(thisMonday());
        assertThat(saved.get(0).getCompleted()).isFalse();
        assertThat(saved.get(0).getAccount()).isSameAs(account);
    }

    @Test
    void nenhumaMetaIncompleta_naoChamaSaveAll() {
        when(weeklyGoalRepository.findAllByWeekStartDateAndCompletedFalse(lastMonday()))
                .thenReturn(List.of());

        scheduler.rolloverIncompleteGoals();

        verify(weeklyGoalRepository, never()).saveAll(any());
    }

    @Test
    void jaExisteMetaComMesmoTituloNaSemanaAtual_naoDuplica() {
        Account account = account(1L);
        WeeklyGoal incomplete = goal(account, "Treinar 3x", lastMonday(), false);
        WeeklyGoal alreadyRolled = goal(account, "Treinar 3x", thisMonday(), false);

        when(weeklyGoalRepository.findAllByWeekStartDateAndCompletedFalse(lastMonday()))
                .thenReturn(List.of(incomplete));
        when(weeklyGoalRepository.findAllByAccount_IdAndWeekStartDate(1L, thisMonday()))
                .thenReturn(List.of(alreadyRolled));

        scheduler.rolloverIncompleteGoals();

        ArgumentCaptor<List<WeeklyGoal>> captor = savedListCaptor();
        verify(weeklyGoalRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).isEmpty();
    }
}
