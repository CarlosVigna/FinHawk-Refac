package com.carlos.finhawk_refac.entity;

import com.carlos.finhawk_refac.enums.Periodicity;
import com.carlos.finhawk_refac.enums.StatusBill;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;


@Entity
@Table(name ="bill")
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Setter
@Getter

public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bill")
    private Long id;

    @Column(name = "description")
    private String description;

    @Column(name = "maturity")
    private LocalDate maturity;

    @Column(name = "emission")
    private LocalDate emission;

    @Column(name = "installment_amount")
    private Integer installmentAmount;

    @Column(name = "parcel_number")
    private Integer parcelNumber;

    @Column(name = "periodicity")
    private Periodicity periodicity;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private StatusBill status = StatusBill.PENDING;
}