package com.kgdev.paymentservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@AllArgsConstructor
@Builder
@NoArgsConstructor
@Data
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

     String razorpayOrderId;
    String razorpayPaymentId;

    @Column(nullable = false)
    String accountNumber;

    @Column(nullable = false,precision=15,scale=2)
    BigDecimal amount;
    @Column(nullable=false)
    String currency;
    @Enumerated(EnumType.STRING)
    PaymentStatus status;
    String description;
    String failureReason;
    @CreationTimestamp
    Instant createdAt;
    @UpdateTimestamp
     Instant updatedAt;


}
