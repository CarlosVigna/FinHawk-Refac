package com.carlos.finhawk_refac.entity;

import com.carlos.finhawk_refac.enums.DayType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Escolha manual do tipo de dia (plantao/folga) pra uma data especifica,
// com prioridade sobre o calculo automatico de DayTypeService -- o
// revezamento fixo e so uma base, na pratica o usuario troca de plantao e
// precisa poder corrigir. So cobre a dimensao PLANTAO/FOLGA -- ENTREGA/
// FIM_DE_SEMANA continua sempre calculado pelo dia da semana, sem override.
@Entity
@Table(name = "day_type_override")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class DayTypeOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_type", nullable = false)
    private DayType dayType;

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
