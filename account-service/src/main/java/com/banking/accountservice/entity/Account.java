package com.banking.accountservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.processing.Generated;
import java.lang.reflect.GenericArrayType;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class Account {
    @Id
    @GeneratedValue(strategy= GenerationType.UUID)
    String id;
    @Column(nullable=false,unique=true)

    String accountNumber;
    @Column(nullable = false)
    String accountHolderName;
    @Column(nullable = false)
    String email;
    @Column(nullable = false)
    String phone;

    @Enumerated(EnumType.STRING)
            @Column(nullable = false)
    AccountType accountType;
    @Enumerated(EnumType.STRING)
            @Column(nullable = false)
    AccountStatus accountStatus;

    @Column(nullable = false, precision=15, scale=2)
    BigDecimal balance;

    @Column(nullable = false, precision=15, scale=2)
    BigDecimal dailyTransactionLimit;

    @PrePersist
            public void prePersist() {
        createdAt = Instant.now();
    }
    Instant createdAt;
    @PreUpdate
            public void preUpdate() {
        updatedAt = Instant.now();
    }
    Instant updatedAt;

}
