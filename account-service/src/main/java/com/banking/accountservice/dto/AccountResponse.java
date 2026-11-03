package com.banking.accountservice.dto;

import com.banking.accountservice.entity.AccountStatus;
import com.banking.accountservice.entity.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
public class AccountResponse {
    String id;
    String accountNumber;
    String accountHolderName;
    String email;
    String phone;
    AccountType accountType;
    AccountStatus accountStatus;
    BigDecimal balance;
    BigDecimal dailyTransactionLimit;
    Instant createdAt;
}
