package com.vitryne.api.store.entity;

import com.vitryne.api.auth.enums.BankAccountType;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "store_bank_account")
public class StoreBankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "holder_name", nullable = false, length = 150)
    private String holderName;

    @Column(name = "holder_document", nullable = false, length = 14)
    private String holderDocument;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private BankAccountType accountType;

    @Column(name = "bank_code", nullable = false, length = 20)
    private String bankCode;

    @Column(name = "agency", nullable = false, length = 20)
    private String agency;

    @Column(name = "account_number", nullable = false, length = 30)
    private String accountNumber;

    @Column(name = "pix_key", length = 255)
    private String pixKey;
}