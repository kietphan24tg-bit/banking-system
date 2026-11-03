package com.kgdev.transactionservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TransactionRequest {
    @NotBlank(message = "Sender account number is required")
    String senderAccountNumber;
    @NotBlank(message = "Receiver account number is required")
    String receiverAccountNumber;
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    BigDecimal amount;
    String description;
}
