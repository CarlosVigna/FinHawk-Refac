package com.carlos.finhawk_refac.entity;

import com.carlos.finhawk_refac.enums.AgendaCompletionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "agenda_event_completion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class AgendaEventCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "agenda_event_id")
    private AgendaEvent agendaEvent;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgendaCompletionStatus status;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    // Somente HABIT+DONE (ver AgendaNotificationScheduler.habitCompletionDigest).
    // Nulo = ainda nao entrou em nenhum resumo consolidado (4h/8h/12h/20h);
    // evento pontual notifica na hora e nunca usa este campo.
    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;
}
