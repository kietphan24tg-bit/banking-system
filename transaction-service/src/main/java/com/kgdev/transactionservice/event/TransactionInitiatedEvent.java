package com.kgdev.transactionservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionInitiatedEvent {
    String transactionId;
    String senderAccountNumber;
    String receiverAccountNumber;
    BigDecimal amount;
    String description;
}
