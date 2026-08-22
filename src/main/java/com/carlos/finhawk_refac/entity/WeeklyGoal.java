package com.carlos.finhawk_refac.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "weekly_goal")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class WeeklyGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    // Segunda-feira da semana em questao -- "semana atual" e so filtrar por
    // esse valor batendo com a segunda desta semana. Metas de semanas
    // passadas continuam no banco (historico), so nao aparecem mais nas
    // consultas de semana atual.
    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(nullable = false)
    private Boolean completed = false;

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
