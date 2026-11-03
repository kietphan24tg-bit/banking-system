package com.banking.accountservice.controller;

import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.entity.Account;
import com.banking.accountservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.function.EntityResponse;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest account) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.createAccount(account));
    }

    @GetMapping("{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable("accountNumber") String accountNumber) {
        return ResponseEntity.ok(accountService.getAccount(accountNumber));
    }

    @GetMapping("{accountNumber}/balance")
    public ResponseEntity<?> getBalance(@PathVariable("accountNumber") String accountNumber) {
        return ResponseEntity.ok(accountService.getBalance(accountNumber));
    }


    @PutMapping("{accountNumber}/block")
    public ResponseEntity<?> blockAccount(@PathVariable("accountNumber") String accountNumber) {
        accountService.blockAccount(accountNumber);
        return ResponseEntity.ok("Account blocked successfully");
    }

    @PutMapping("/${accountNumber}/deduct")
    public ResponseEntity<String> deductBalance(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount
            ){
        accountService.deductBalance(accountNumber,amount);
        return ResponseEntity.ok("Account deducted successfully");
    }

    @PutMapping("/${accountNumber}/credit")
    public ResponseEntity<String> creditBalance(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount
    ){
        accountService.creditBalance(accountNumber,amount);
        return ResponseEntity.ok("Account deducted successfully");
    }


}
