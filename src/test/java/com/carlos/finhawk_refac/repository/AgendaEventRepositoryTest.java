package com.carlos.finhawk_refac.repository;

import com.carlos.finhawk_refac.entity.Account;
import com.carlos.finhawk_refac.entity.AgendaEvent;
import com.carlos.finhawk_refac.entity.UserAccount;
import com.carlos.finhawk_refac.enums.AgendaEventType;
import com.carlos.finhawk_refac.enums.RecurrenceFrequency;
import com.carlos.finhawk_refac.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Bug relatado: habitos/eventos nao apareciam ordenados por horario na
// listagem principal (pagina de Habitos e "Todos os habitos") -- a causa
// raiz era essa query sem ORDER BY nenhum, devolvendo na ordem que o banco
// quisesse (tipicamente insercao/PK, nao horario). Confirma contra um banco
// real (H2 de teste) que salvar fora de ordem nao muda o resultado.
//
// @DataJpaTest nao esta disponivel nesse setup modular do Spring Boot 4 (o
// mesmo motivo pelo qual WhatsAppNotificationService injeta seu proprio
// ObjectMapper em vez do bean do Spring) -- usa @SpringBootTest +
// @Transactional (rollback automatico por teste), mesmo padrao ja usado em
// IsolationBetweenUsersTest.
@SpringBootTest
@Transactional
class AgendaEventRepositoryTest {

    @Autowired
    private AgendaEventRepository agendaEventRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;

    private Account persistAccount() {
        UserAccount user = new UserAccount();
        user.setName("Teste Ordenacao");
        user.setEmail("teste-ordering@finhawk.app");
        user.setPassword("hash");
        user.setRole(UserRole.VIEWER);
        user = userAccountRepository.save(user);

        Account account = new Account();
        account.setName("Conta Teste Ordenacao");
        account.setUserAccount(user);
        return accountRepository.save(account);
    }

    private AgendaEvent habit(Account account, String title, LocalTime time) {
        AgendaEvent h = new AgendaEvent();
        h.setTitle(title);
        h.setAccount(account);
        h.setType(AgendaEventType.HABIT);
        h.setRecurrenceFrequency(RecurrenceFrequency.DAILY);
        h.setTimeOfDay(time);
        h.setActive(true);
        return h;
    }

    private AgendaEvent event(Account account, String title, LocalDateTime dateTime) {
        AgendaEvent e = new AgendaEvent();
        e.setTitle(title);
        e.setAccount(account);
        e.setType(AgendaEventType.ONE_TIME);
        e.setEventDateTime(dateTime);
        e.setActive(true);
        return e;
    }

    @Test
    void habitosSalvosForaDeOrdem_vemOrdenadosPorTimeOfDay() {
        Account account = persistAccount();

        // Salva de proposito fora de ordem (20h, 7h, 14h) pra garantir que o
        // resultado nao esteja so "por acaso" na ordem de insercao.
        agendaEventRepository.saveAll(List.of(
                habit(account, "Noite", LocalTime.of(20, 0)),
                habit(account, "Manha", LocalTime.of(7, 0)),
                habit(account, "Tarde", LocalTime.of(14, 0))
        ));

        List<AgendaEvent> result = agendaEventRepository
                .findAllByAccount_IdAndTypeAndDeletedAtIsNullOrderByEventDateTimeAscTimeOfDayAsc(
                        account.getId(), AgendaEventType.HABIT);

        assertThat(result).extracting(AgendaEvent::getTitle).containsExactly("Manha", "Tarde", "Noite");
    }

    @Test
    void eventosSalvosForaDeOrdem_vemOrdenadosPorEventDateTime() {
        Account account = persistAccount();

        agendaEventRepository.saveAll(List.of(
                event(account, "Noite", LocalDateTime.of(2026, 8, 20, 20, 0)),
                event(account, "Manha", LocalDateTime.of(2026, 8, 20, 7, 0)),
                event(account, "Tarde", LocalDateTime.of(2026, 8, 20, 14, 0))
        ));

        List<AgendaEvent> result = agendaEventRepository
                .findAllByAccount_IdAndTypeAndDeletedAtIsNullOrderByEventDateTimeAscTimeOfDayAsc(
                        account.getId(), AgendaEventType.ONE_TIME);

        assertThat(result).extracting(AgendaEvent::getTitle).containsExactly("Manha", "Tarde", "Noite");
    }

    @Test
    void listaSemFiltroDeTipo_misturaEventosEHabitosMasContinuaConsistente() {
        Account account = persistAccount();

        agendaEventRepository.saveAll(List.of(
                habit(account, "Habito Noite", LocalTime.of(20, 0)),
                event(account, "Evento Manha", LocalDateTime.of(2026, 8, 20, 7, 0)),
                habit(account, "Habito Tarde", LocalTime.of(14, 0))
        ));

        // Sem filtro de tipo: eventDateTime e nulo pra habito e timeOfDay e
        // nulo pra evento, entao o segundo criterio de ordenacao e quem
        // resolve dentro de cada grupo -- aqui so confirma que a query nao
        // quebra e devolve todo mundo.
        List<AgendaEvent> result = agendaEventRepository
                .findAllByAccount_IdAndDeletedAtIsNullOrderByEventDateTimeAscTimeOfDayAsc(account.getId());

        assertThat(result).hasSize(3);
    }
}
