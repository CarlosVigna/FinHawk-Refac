package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Testa a logica de janela/limite isoladamente, com um Clock controlado --
// evita precisar esperar 15 minutos de verdade pra confirmar que a janela
// expira e o bloqueio reseta.
class LoginAttemptServiceTest {

    private static final String IP = "203.0.113.10";
    private static final String EMAIL = "vitima@teste.com";

    private LoginAttemptService newService(Instant now) {
        LoginAttemptService service = new LoginAttemptService();
        service.setClockForTesting(Clock.fixed(now, ZoneOffset.UTC));
        return service;
    }

    @Test
    void permiteAte5TentativasFalhas() {
        LoginAttemptService service = newService(Instant.parse("2026-01-01T10:00:00Z"));

        for (int i = 0; i < 5; i++) {
            assertThatCode(() -> service.checkAllowed(IP, EMAIL)).doesNotThrowAnyException();
            service.registerFailure(IP, EMAIL);
        }
    }

    @Test
    void sextaTentativaDentroDaJanela_lancaTooManyRequests() {
        LoginAttemptService service = newService(Instant.parse("2026-01-01T10:00:00Z"));

        for (int i = 0; i < 5; i++) {
            service.registerFailure(IP, EMAIL);
        }

        assertThatThrownBy(() -> service.checkAllowed(IP, EMAIL))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void loginComSucesso_resetaImediatamenteOContador() {
        LoginAttemptService service = newService(Instant.parse("2026-01-01T10:00:00Z"));

        for (int i = 0; i < 5; i++) {
            service.registerFailure(IP, EMAIL);
        }
        assertThatThrownBy(() -> service.checkAllowed(IP, EMAIL)).isInstanceOf(TooManyRequestsException.class);

        service.registerSuccess(IP, EMAIL);

        assertThatCode(() -> service.checkAllowed(IP, EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void aposJanelaDe15MinutosExpirar_bloqueioReseta() {
        Instant inicio = Instant.parse("2026-01-01T10:00:00Z");
        LoginAttemptService service = newService(inicio);

        for (int i = 0; i < 5; i++) {
            service.registerFailure(IP, EMAIL);
        }
        assertThatThrownBy(() -> service.checkAllowed(IP, EMAIL)).isInstanceOf(TooManyRequestsException.class);

        // Avanca o relogio simulado pra 15 minutos e 1 segundo depois --
        // janela expirada, deveria liberar de novo.
        service.setClockForTesting(Clock.fixed(inicio.plusSeconds(15 * 60 + 1), ZoneOffset.UTC));

        assertThatCode(() -> service.checkAllowed(IP, EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void bloqueiaPorEmailMesmoTrocandoDeIp() {
        LoginAttemptService service = newService(Instant.parse("2026-01-01T10:00:00Z"));

        for (int i = 0; i < 5; i++) {
            service.registerFailure("203.0.113." + i, EMAIL);
        }

        assertThatThrownBy(() -> service.checkAllowed("203.0.113.99", EMAIL))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void bloqueiaPorIpMesmoTrocandoDeEmail() {
        LoginAttemptService service = newService(Instant.parse("2026-01-01T10:00:00Z"));

        for (int i = 0; i < 5; i++) {
            service.registerFailure(IP, "email" + i + "@teste.com");
        }

        assertThatThrownBy(() -> service.checkAllowed(IP, "email-novo@teste.com"))
                .isInstanceOf(TooManyRequestsException.class);
    }
}
