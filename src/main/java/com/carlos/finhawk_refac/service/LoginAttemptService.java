package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.exception.TooManyRequestsException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// Rate limit em memoria (uma instancia do backend no Railway, sem Redis) --
// bloqueia por IP e por e-mail separadamente, o que for atingido primeiro,
// pra cobrir tanto "um IP tentando varios e-mails" quanto "varios IPs
// tentando o mesmo e-mail".
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    // Clock injetavel so pra permitir testar a expiracao da janela sem
    // precisar esperar 15 minutos de verdade -- em producao e sempre
    // Clock.systemUTC(), comportamento identico ao Instant.now() de antes.
    private Clock clock = Clock.systemUTC();

    private record Attempts(AtomicInteger count, Instant windowStart) {
        boolean expired(Instant now) {
            return Duration.between(windowStart, now).compareTo(WINDOW) >= 0;
        }
    }

    private final ConcurrentHashMap<String, Attempts> attemptsByKey = new ConcurrentHashMap<>();

    public void checkAllowed(String ip, String email) {
        checkKey("ip:" + ip);
        checkKey("email:" + normalizeEmail(email));
    }

    public void registerFailure(String ip, String email) {
        registerFailureForKey("ip:" + ip);
        registerFailureForKey("email:" + normalizeEmail(email));
    }

    public void registerSuccess(String ip, String email) {
        attemptsByKey.remove("ip:" + ip);
        attemptsByKey.remove("email:" + normalizeEmail(email));
    }

    void setClockForTesting(Clock clock) {
        this.clock = clock;
    }

    private void checkKey(String key) {
        Instant now = Instant.now(clock);
        Attempts attempts = attemptsByKey.get(key);
        if (attempts != null && !attempts.expired(now) && attempts.count().get() >= MAX_ATTEMPTS) {
            throw new TooManyRequestsException("Muitas tentativas de login. Tente novamente em alguns minutos.");
        }
    }

    private void registerFailureForKey(String key) {
        Instant now = Instant.now(clock);
        attemptsByKey.compute(key, (k, existing) -> {
            if (existing == null || existing.expired(now)) {
                return new Attempts(new AtomicInteger(1), now);
            }
            existing.count().incrementAndGet();
            return existing;
        });
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
