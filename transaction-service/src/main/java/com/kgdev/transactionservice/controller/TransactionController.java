package com.kgdev.transactionservice.controller;

import com.kgdev.transactionservice.dto.TransactionRequest;
import com.kgdev.transactionservice.dto.TransactionResponse;
import com.kgdev.transactionservice.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer( @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.transfer(request));
    }
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable("transactionId") String transactionId) {
        return ResponseEntity.ok(transactionService.getTransaction(transactionId));
    }
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<TransactionResponse> getTransactionHistory(@PathVariable("accountNumber") String accountNumber) {
        return ResponseEntity.ok(transactionService.getTransactionHistory(accountNumber));
    }

    @PostMapping("/{transactionId}/verify")
    public ResponseEntity<TransactionResponse> verifyTransaction(
            @PathVariable("transactionId") String transactionId,
            @RequestParam String otp
    ) {
        log.info("Verify transaction with id {}", transactionId);
        return ResponseEntity.ok(transactionService.verifyOTP(transactionId,otp));
    }


 }
