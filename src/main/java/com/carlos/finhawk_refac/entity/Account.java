package com.carlos.finhawk_refac.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "account")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_account")
    private Long id;

    @Column(name = "name_account")
    private String name;

    @Column(name = "photo_url_account")
    private String photoUrl;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserAccount userAccount;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    private List<Bill> bills;
}