package com.carlos.finhawk_refac.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "checklist_completion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class ChecklistCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "checklist_item_id")
    private ChecklistItem checklistItem;

    @Column(nullable = false, length = 7)
    private String month;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;
}
