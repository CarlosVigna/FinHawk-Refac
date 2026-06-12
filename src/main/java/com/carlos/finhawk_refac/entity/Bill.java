package com.carlos.finhawk_refac.entity;

import com.carlos.finhawk_refac.enums.Periodicity;
import com.carlos.finhawk_refac.enums.StatusBill;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bill")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bill")
    private Long id;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private LocalDate emission;

    @Column(nullable = false)
    private LocalDate maturity;

    @Column(name = "installment_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal installmentAmount;

    @Column(name = "parcel_number")
    private Integer installmentCount;

    // Parcela atual (1, 2, 3...)
    @Column(name = "current_installment")
    private Integer currentInstallment = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "periodicity", nullable = false)
    private Periodicity periodicity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusBill status = StatusBill.PENDING;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        // paidAt/receivedAt remain null until status change
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}