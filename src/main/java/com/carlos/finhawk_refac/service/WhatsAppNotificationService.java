package com.carlos.finhawk_refac.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

// Envia mensagens de texto pro grupo de WhatsApp da familia via Evolution
// API. Falha de envio (API fora do ar, numero desconectado, etc.) e sempre
// logada e engolida aqui -- uma notificacao que nao sai NUNCA pode quebrar
// o resto do sistema (schedulers, requests HTTP normais).
@Service
public class WhatsAppNotificationService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppNotificationService.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    // Instancia propria em vez de injetar o bean do Spring: o projeto usa o
    // starter modular spring-boot-starter-webmvc (Boot 4), que nao expoe um
    // ObjectMapper autoconfigurado (isso vem de spring-boot-starter-json,
    // que nao esta nas dependencias) -- injetar quebraria o contexto.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${whatsapp.api.url:}")
    private String apiUrl;

    @Value("${whatsapp.api.key:}")
    private String apiKey;

    @Value("${whatsapp.instance.name:}")
    private String instanceName;

    @Value("${whatsapp.group.id:}")
    private String groupId;

    private record SendTextRequest(String number, String text) {}

    public void sendMessage(String text) {
        if (apiUrl == null || apiUrl.isBlank() || instanceName == null || instanceName.isBlank()
                || groupId == null || groupId.isBlank()) {
            log.warn("WhatsApp nao configurado (WHATSAPP_API_URL/WHATSAPP_INSTANCE_NAME/WHATSAPP_GROUP_ID ausentes) -- notificacao nao enviada.");
            return;
        }

        try {
            String body = objectMapper.writeValueAsString(new SendTextRequest(groupId, text));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/message/sendText/" + instanceName))
                    .header("Content-Type", "application/json")
                    .header("apikey", apiKey)
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Notificacao WhatsApp enviada (status {}).", response.statusCode());
            } else {
                log.error("Falha ao enviar notificacao WhatsApp -- status {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Falha ao enviar notificacao WhatsApp: {}", e.getMessage(), e);
        }
    }
}
