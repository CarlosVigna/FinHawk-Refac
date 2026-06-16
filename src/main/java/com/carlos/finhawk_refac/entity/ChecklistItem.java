package com.carlos.finhawk_refac.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "checklist_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class ChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_checklist_item")
    private Long id;

    @Column(nullable = false)
    private String description;

    @Column(name = "due_day", nullable = false)
    private Integer dueDay;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "approximate_value")
    private BigDecimal approximateValue;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id")
    private Account account;
}