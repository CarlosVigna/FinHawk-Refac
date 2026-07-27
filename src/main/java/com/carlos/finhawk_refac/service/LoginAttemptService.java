package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.exception.TooManyRequestsException;
import org.springframework.stereotype.Service;

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

    private record Attempts(AtomicInteger count, Instant windowStart) {
        boolean expired() {
            return Duration.between(windowStart, Instant.now()).compareTo(WINDOW) >= 0;
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

    private void checkKey(String key) {
        Attempts attempts = attemptsByKey.get(key);
        if (attempts != null && !attempts.expired() && attempts.count().get() >= MAX_ATTEMPTS) {
            throw new TooManyRequestsException("Muitas tentativas de login. Tente novamente em alguns minutos.");
        }
    }

    private void registerFailureForKey(String key) {
        attemptsByKey.compute(key, (k, existing) -> {
            if (existing == null || existing.expired()) {
                return new Attempts(new AtomicInteger(1), Instant.now());
            }
            existing.count().incrementAndGet();
            return existing;
        });
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
