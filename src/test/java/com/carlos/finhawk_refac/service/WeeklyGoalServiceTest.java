package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.dto.request.WeeklyGoalRequestDTO;
import com.carlos.finhawk_refac.dto.response.WeeklyGoalResponseDTO;
import com.carlos.finhawk_refac.entity.Account;
import com.carlos.finhawk_refac.entity.UserAccount;
import com.carlos.finhawk_refac.entity.WeeklyGoal;
import com.carlos.finhawk_refac.repository.AccountRepository;
import com.carlos.finhawk_refac.repository.WeeklyGoalRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Cobre a edicao (title/description) e o campo description novo em
// WeeklyGoal -- em especial que a descricao entra nas notificacoes de
// criar/editar/concluir/apagar quando preenchida, e some da mensagem
// quando ausente (mesmo padrao ja usado pra AgendaEvent).
@ExtendWith(MockitoExtension.class)
class WeeklyGoalServiceTest {

    @Mock private WeeklyGoalRepository weeklyGoalRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private CrudNotificationService crudNotificationService;

    private WeeklyGoalService service;
    private UserAccount currentUser;
    private Account account;

    @BeforeEach
    void setUp() {
        service = new WeeklyGoalService(weeklyGoalRepository, accountRepository, auditLogService, crudNotificationService);

        currentUser = new UserAccount();
        currentUser.setId(1L);
        currentUser.setEmail("teste@finhawk.app");

        account = new Account();
        account.setId(10L);
        account.setUserAccount(currentUser);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null, currentUser.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private String captureNotification() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(crudNotificationService).notify(captor.capture());
        return captor.getValue();
    }

    @Test
    void create_comDescricao_salvaEIncluiNaNotificacao() {
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(weeklyGoalRepository.save(any(WeeklyGoal.class))).thenAnswer(inv -> {
            WeeklyGoal g = inv.getArgument(0);
            g.setId(5L);
            return g;
        });

        WeeklyGoalResponseDTO result = service.create(new WeeklyGoalRequestDTO("Treinar 3x", "Academia + corrida", 10L));

        assertThat(result.title()).isEqualTo("Treinar 3x");
        assertThat(result.description()).isEqualTo("Academia + corrida");
        assertThat(captureNotification()).contains("Treinar 3x").contains("Academia + corrida");
    }

    @Test
    void create_semDescricao_naoQuebraENaoMostraLinhaVazia() {
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(weeklyGoalRepository.save(any(WeeklyGoal.class))).thenAnswer(inv -> {
            WeeklyGoal g = inv.getArgument(0);
            g.setId(5L);
            return g;
        });

        service.create(new WeeklyGoalRequestDTO("Ler um livro", null, 10L));

        String message = captureNotification();
        assertThat(message).contains("Ler um livro");
        // sem descricao, so duas linhas (cabecalho + titulo), sem terceira linha vazia
        assertThat(message.split("\n")).hasSize(2);
    }

    @Test
    void update_alteraTituloEDescricao() {
        WeeklyGoal existing = new WeeklyGoal();
        existing.setId(5L);
        existing.setTitle("Título antigo");
        existing.setDescription("Descrição antiga");
        existing.setAccount(account);
        existing.setCompleted(false);

        when(weeklyGoalRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(weeklyGoalRepository.save(any(WeeklyGoal.class))).thenAnswer(inv -> inv.getArgument(0));

        WeeklyGoalResponseDTO result = service.update(5L, new WeeklyGoalRequestDTO("Título novo", "Descrição nova", 10L));

        assertThat(result.title()).isEqualTo("Título novo");
        assertThat(result.description()).isEqualTo("Descrição nova");
        assertThat(captureNotification()).contains("Título novo").contains("Descrição nova");
    }

    @Test
    void update_metaDeOutraConta_negaAcesso() {
        UserAccount outroUsuario = new UserAccount();
        outroUsuario.setId(2L);
        Account contaDeOutraPessoa = new Account();
        contaDeOutraPessoa.setId(99L);
        contaDeOutraPessoa.setUserAccount(outroUsuario);

        WeeklyGoal existing = new WeeklyGoal();
        existing.setId(5L);
        existing.setAccount(contaDeOutraPessoa);

        when(weeklyGoalRepository.findById(5L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(5L, new WeeklyGoalRequestDTO("X", null, 99L)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void delete_comDescricao_incluiNaNotificacao() {
        WeeklyGoal existing = new WeeklyGoal();
        existing.setId(5L);
        existing.setTitle("Meditar");
        existing.setDescription("10 minutos por dia");
        existing.setAccount(account);

        when(weeklyGoalRepository.findById(5L)).thenReturn(Optional.of(existing));

        service.delete(5L);

        assertThat(captureNotification()).contains("Meditar").contains("10 minutos por dia");
    }
}
