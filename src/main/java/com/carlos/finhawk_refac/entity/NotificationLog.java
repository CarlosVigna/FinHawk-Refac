package com.carlos.finhawk_refac.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Registra que um job de notificacao (resumo noturno, semanal, aviso
// matinal) ja rodou pra uma determinada data de referencia -- protege
// contra duplicidade se o scheduler disparar de novo (ex: restart do
// servico) sem depender de estado em memoria, que nao sobrevive a restart.
@Entity
@Table(name = "notification_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_key", nullable = false)
    private String jobKey;

    @Column(name = "reference_date", nullable = false)
    private LocalDate referenceDate;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;
}
