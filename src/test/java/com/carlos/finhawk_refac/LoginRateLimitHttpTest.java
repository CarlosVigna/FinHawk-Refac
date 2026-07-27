package com.carlos.finhawk_refac;

import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Confirma o fio inteiro (AuthenticationController -> LoginAttemptService ->
// GlobalExceptionHandler) contra o endpoint HTTP real: a 6a tentativa de
// login errada dentro da janela deve responder 429, nao 401 nem 500.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoginRateLimitHttpTest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate rest = new TestRestTemplate();

    @Test
    void sextaTentativaDeLoginErradaRetorna429() {
        String email = "rate-limit-" + System.nanoTime() + "@teste.finhawk.app";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> credenciaisErradas = Map.of("email", email, "password", "senhaErrada123");
        HttpEntity<Map<String, String>> request = new HttpEntity<>(credenciaisErradas, headers);

        ResponseEntity<String> ultimaResposta = null;
        for (int tentativa = 1; tentativa <= 6; tentativa++) {
            ultimaResposta = rest.postForEntity(
                    "http://localhost:" + port + "/auth/login", request, String.class);

            if (tentativa <= 5) {
                assertThat(ultimaResposta.getStatusCode())
                        .as("tentativa %d deveria ser 401 (credenciais invalidas), nao bloqueio", tentativa)
                        .isEqualTo(HttpStatus.UNAUTHORIZED);
            }
        }

        assertThat(ultimaResposta.getStatusCode())
                .as("6a tentativa dentro da janela deveria ser bloqueada com 429")
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}
