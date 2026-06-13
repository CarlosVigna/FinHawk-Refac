package com.carlos.finhawk_refac.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${mail.from.address}")
    private String fromAddress;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String to, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject("Recuperacao de senha — FinHawk");
            message.setText(
                "Ola,\n\n" +
                "Recebemos uma solicitacao de recuperacao de senha para sua conta no FinHawk.\n\n" +
                "Clique no link abaixo para redefinir sua senha:\n" +
                frontendUrl + "/reset-senha?token=" + token + "\n\n" +
                "O link expira em 30 minutos.\n\n" +
                "Se voce nao solicitou a recuperacao de senha, ignore este e-mail.\n\n" +
                "FinHawk"
            );
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail de recuperacao de senha para {}: {}", to, e.getMessage());
            throw e;
        }
    }
}
