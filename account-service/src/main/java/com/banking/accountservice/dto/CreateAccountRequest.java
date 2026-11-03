package com.banking.accountservice.dto;

import com.banking.accountservice.entity.AccountType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountRequest {
    @NotBlank(message = "Account holder name is required")
     String accountHolderName;
    @Email(message = "Invalid email format")
     String email;
    @NotBlank(message = "Phone is required")
     String phone;
    @NotNull(message = "Account type is required")
     AccountType accountType;
    @Positive(message = "Initial deposit must be positive ")
     BigDecimal initialDeposit;
}
