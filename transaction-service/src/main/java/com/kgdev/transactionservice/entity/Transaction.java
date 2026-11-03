package com.kgdev.transactionservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(nullable = false)
    String senderAccountNumber;
    @Column(nullable = false)
    String receiverAccountNumber;
    @Column(nullable = false,precision=15,scale=2)
    BigDecimal amount;
    @Column(nullable = false)
            @Enumerated(EnumType.STRING)
    TransactionType type;
    @Column(nullable = false)
            @Enumerated(EnumType.STRING)
    TransactionStatus status;

    String description;
    String failureReason;
    String referenceNumber;
    Instant creationAt;
    Instant completeAt;
}
