package com.carlos.finhawk_refac.entity;

import com.carlos.finhawk_refac.enums.AgendaEventType;
import com.carlos.finhawk_refac.enums.RecurrenceFrequency;
import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "agenda_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class AgendaEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgendaEventType type;

    // Somente para ONE_TIME
    @Column(name = "event_date_time")
    private LocalDateTime eventDateTime;

    // Somente para HABIT
    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_frequency")
    private RecurrenceFrequency recurrenceFrequency;

    // Somente para HABIT com recurrenceFrequency = WEEKLY
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "agenda_event_day_of_week", joinColumns = @JoinColumn(name = "agenda_event_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private List<DayOfWeek> daysOfWeek;

    // Somente para HABIT
    @Column(name = "time_of_day")
    private LocalTime timeOfDay;

    // Permite pausar um habito sem apagar o historico de completions
    @Column(nullable = false)
    private Boolean active = true;

    // Dedup do lembrete "1h antes" (somente ONE_TIME)
    @Column(name = "reminder_sent_at")
    private LocalDateTime reminderSentAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
