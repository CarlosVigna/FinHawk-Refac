package com.carlos.finhawk_refac;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Item mais critico da auditoria de seguranca: confirmar de ponta a ponta
// (registro real, login real, chamadas HTTP reais com o token de cada
// usuario) que o usuario A nunca consegue ler, editar ou apagar um recurso
// do usuario B em bill/account/category -- nem por acidente de digitar o id
// errado.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IsolationBetweenUsersTest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate rest = new TestRestTemplate();

    private String tokenA;
    private String tokenB;
    private Long accountIdA;
    private Long categoryIdA;
    private Long billIdA;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) headers.setBearerAuth(token);
        return headers;
    }

    private String registerAndLogin(String emailPrefix) {
        String email = emailPrefix + "-" + System.nanoTime() + "@teste.finhawk.app";

        Map<String, String> register = new HashMap<>();
        register.put("name", "Usuario " + emailPrefix);
        register.put("email", email);
        register.put("password", "senhaForte123");

        ResponseEntity<String> registerResponse = rest.postForEntity(
                url("/auth/register"), new HttpEntity<>(register, authHeaders(null)), String.class);
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Map<String, String> login = new HashMap<>();
        login.put("email", email);
        login.put("password", "senhaForte123");

        ResponseEntity<Map> loginResponse = rest.postForEntity(
                url("/auth/login"), new HttpEntity<>(login, authHeaders(null)), Map.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        return (String) loginResponse.getBody().get("token");
    }

    @BeforeEach
    void setUp() {
        tokenA = registerAndLogin("usuarioA");
        tokenB = registerAndLogin("usuarioB");

        // Usuario A cria conta, categoria e lancamento -- dados que o
        // usuario B NUNCA deve conseguir ler/editar/apagar.
        Map<String, String> accountBody = Map.of("name", "Conta Secreta de A", "photoUrl", "");
        ResponseEntity<Map> accountResponse = rest.postForEntity(
                url("/account"), new HttpEntity<>(accountBody, authHeaders(tokenA)), Map.class);
        assertThat(accountResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        accountIdA = Long.valueOf(accountResponse.getBody().get("id").toString());

        Map<String, Object> categoryBody = new HashMap<>();
        categoryBody.put("name", "Categoria Secreta de A");
        categoryBody.put("type", "PAYMENT");
        categoryBody.put("accountId", accountIdA);
        ResponseEntity<Map> categoryResponse = rest.postForEntity(
                url("/category"), new HttpEntity<>(categoryBody, authHeaders(tokenA)), Map.class);
        assertThat(categoryResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        categoryIdA = Long.valueOf(categoryResponse.getBody().get("id").toString());

        Map<String, Object> billBody = new HashMap<>();
        billBody.put("description", "Lancamento Secreto de A");
        billBody.put("emission", "2026-07-01");
        billBody.put("maturity", "2026-07-10");
        billBody.put("installmentAmount", "500.00");
        billBody.put("installmentCount", 1);
        billBody.put("periodicity", "MONTHLY");
        billBody.put("categoryId", categoryIdA);
        billBody.put("accountId", accountIdA);
        ResponseEntity<Map> billResponse = rest.postForEntity(
                url("/bill"), new HttpEntity<>(billBody, authHeaders(tokenA)), Map.class);
        assertThat(billResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        billIdA = Long.valueOf(billResponse.getBody().get("id").toString());
    }

    @Test
    void usuarioB_naoConsegueLerContaDeUsuarioA() {
        ResponseEntity<String> response = rest.exchange(
                url("/account/" + accountIdA), HttpMethod.GET, new HttpEntity<>(authHeaders(tokenB)), String.class);

        assertThat(response.getStatusCode().value()).isIn(403, 404);
        assertThat(response.getBody()).doesNotContain("Conta Secreta de A");
    }

    @Test
    void usuarioB_naoConsegueEditarContaDeUsuarioA() {
        Map<String, String> tentativaEdicao = Map.of("name", "Conta Sequestrada", "photoUrl", "");

        ResponseEntity<String> response = rest.exchange(
                url("/account/" + accountIdA), HttpMethod.PUT,
                new HttpEntity<>(tentativaEdicao, authHeaders(tokenB)), String.class);

        assertThat(response.getStatusCode().value()).isIn(403, 404);
    }

    @Test
    void usuarioB_naoConsegueApagarContaDeUsuarioA() {
        ResponseEntity<String> response = rest.exchange(
                url("/account/" + accountIdA), HttpMethod.DELETE, new HttpEntity<>(authHeaders(tokenB)), String.class);

        assertThat(response.getStatusCode().value()).isIn(403, 404);

        // Confirma que a conta de A continua existindo e intacta -- o
        // isolamento nao pode ser "silenciosamente apagou mas nao contou".
        ResponseEntity<String> confirmacao = rest.exchange(
                url("/account/" + accountIdA), HttpMethod.GET, new HttpEntity<>(authHeaders(tokenA)), String.class);
        assertThat(confirmacao.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(confirmacao.getBody()).contains("Conta Secreta de A");
    }

    @Test
    void usuarioB_naoConsegueLerLancamentoDeUsuarioA() {
        ResponseEntity<String> response = rest.exchange(
                url("/bill/" + billIdA), HttpMethod.GET, new HttpEntity<>(authHeaders(tokenB)), String.class);

        assertThat(response.getStatusCode().value()).isIn(403, 404);
        assertThat(response.getBody()).doesNotContain("Lancamento Secreto de A");
    }

    @Test
    void usuarioB_naoConsegueApagarLancamentoDeUsuarioA() {
        ResponseEntity<String> response = rest.exchange(
                url("/bill/" + billIdA), HttpMethod.DELETE, new HttpEntity<>(authHeaders(tokenB)), String.class);

        assertThat(response.getStatusCode().value()).isIn(403, 404);
    }

    @Test
    void usuarioB_naoConsegueLerCategoriaDeUsuarioA() {
        ResponseEntity<String> response = rest.exchange(
                url("/category/" + categoryIdA), HttpMethod.GET, new HttpEntity<>(authHeaders(tokenB)), String.class);

        assertThat(response.getStatusCode().value()).isIn(403, 404);
        assertThat(response.getBody()).doesNotContain("Categoria Secreta de A");
    }

    @Test
    void usuarioB_naoConsegueApagarCategoriaDeUsuarioA() {
        ResponseEntity<String> response = rest.exchange(
                url("/category/" + categoryIdA), HttpMethod.DELETE, new HttpEntity<>(authHeaders(tokenB)), String.class);

        assertThat(response.getStatusCode().value()).isIn(403, 404);
    }

    @Test
    void semToken_naoAcessaNadaDeNinguem() {
        ResponseEntity<String> response = rest.exchange(
                url("/account/" + accountIdA), HttpMethod.GET, new HttpEntity<>(authHeaders(null)), String.class);

        assertThat(response.getStatusCode().value()).isIn(401, 403);
    }
}
