package com.kgdev.transactionservice.repository;

import com.kgdev.transactionservice.dto.TransactionRequest;
import com.kgdev.transactionservice.dto.TransactionResponse;
import com.kgdev.transactionservice.entity.Transaction;
import com.kgdev.transactionservice.service.TransactionService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Repository

public interface TransactionReposiotry extends JpaRepository<Transaction, String> {
    List<Transaction> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
}
