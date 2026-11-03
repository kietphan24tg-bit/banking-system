package com.kgdev.transactionservice.dto;

import com.kgdev.transactionservice.entity.TransactionStatus;
import com.kgdev.transactionservice.entity.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionResponse {
    String id;
    String senderAccountNumber;
    String receiverAccountNumber;
    BigDecimal amount;
    TransactionType type;
    TransactionStatus status;
    String description;
    String failureReason;
    String referenceNumber;
    Instant creationAt;
    Instant completeAt;

}
