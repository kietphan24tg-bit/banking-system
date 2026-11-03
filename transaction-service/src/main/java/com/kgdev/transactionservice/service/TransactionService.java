package com.kgdev.transactionservice.service;

import com.kgdev.transactionservice.dto.TransactionRequest;
import com.kgdev.transactionservice.dto.TransactionResponse;
import com.kgdev.transactionservice.entity.Transaction;
import com.kgdev.transactionservice.entity.TransactionStatus;
import com.kgdev.transactionservice.entity.TransactionType;
import com.kgdev.transactionservice.event.TransactionInitiatedEvent;
import com.kgdev.transactionservice.event.TransactonCompletedEvent;
import com.kgdev.transactionservice.repository.TransactionReposiotry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionReposiotry transactionReposiotry;
    private final AccountServiceClient accountServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String TRANSACTION_INITIATED_TOPIC = "transaction.initiated";
    private static final String TRANSACTION_COMPLETED_TOPIC = "transaction.completed";
    private static final String TRANSACTION_REFUNDED_TOPIC = "transaction.refunded";
    private static final String FRAUD_DETECTED_TOPIC = "fraud.detected";

    /*
    SAGA STEP - 1: Initiate transfer
     Deducts from sender via feign
     saves transaction as PROCESSING
     Publish event to kafka for fraud check
     Returns
     @Param request
     @return
    * */

    public TransactionResponse transfer(TransactionRequest request) {
        log.info(
            "SAGA START - Transfer: {} -> {} amount {}",
                request.getSenderAccountNumber(),
                request.getReceiverAccountNumber(),
                request.getAmount()
        );
        Transaction transaction = new Transaction();
        transaction.builder()
                .amount(request.getAmount())
                .receiverAccountNumber(request.getSenderAccountNumber())
                .senderAccountNumber(request.getReceiverAccountNumber())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.PROCESSING)
                .description(request.getDescription())
                .referenceNumber(UUID.randomUUID().toString())
                .build();
        Transaction savedTransaction = transactionReposiotry.save(transaction);
        log.info("Transaction save as PROCESSING:{}",savedTransaction.getId());
        TransactionInitiatedEvent event = new TransactionInitiatedEvent(
                savedTransaction.getId(),
                savedTransaction.getSenderAccountNumber(),
                savedTransaction.getReceiverAccountNumber(),
                savedTransaction.getAmount(),
                savedTransaction.getDescription()
        );
        kafkaTemplate.send(TRANSACTION_INITIATED_TOPIC,savedTransaction.getId(), event);
        log.info("SAGA STEP 2 - TransactionInitiatedEvent published {}",savedTransaction.getId());
        return mapToResponse(savedTransaction);
    }
    public TransactionResponse getTransaction(String transactionId) {
        return mapToResponse(
                transactionReposiotry.findById(transactionId)
                        .orElseThrow(()-> new RuntimeException("Transaction not found with id " + transactionId))
        );
    }
    public List<TransactionResponse> getTransactionHistory(String accountNumber){
       List<Transaction> transactionHistory =   transactionReposiotry.findByAccountNumberOrderByCreatedAtDesc(accountNumber);
      return  transactionHistory.stream().map(this::mapToResponse).toList();

    }

    public TransactionResponse verifyOTP(String transactionId, String otp) {
        log.info("OTP verification for the transaction: {}", transactionId);
        Transaction transaction = transactionReposiotry.findById(transactionId).orElseThrow(()-> new RuntimeException("Transaction not found with id " + transactionId));
        String otpKey ="verification:otp" + transactionId;
        String storedOtp = redisTemplate.opsForValue().get(otpKey);
        if(storedOtp == null){
            log.warn("OTP expired for transaction {}", transactionId);
            compensateTransaction(transaction,"OTP expired - transaction cancelled and amount refunded");
            return mapToResponse(transaction);
        }
        if(!otp.equals(storedOtp)){
            log.warn("Wrong OTP - blocking account and refunding: {}",transactionId);
            redisTemplate.delete(otpKey);
            blockAccountAndCompensate(
                    transaction,
                    "Wrong OTP entered - transaction cancelled. "+
                            "account blocked for security"
            );
            return mapToResponse(transaction);
        }
        // OTP correct - complete transaction
        log.info("OTP verified - completing transaction: {}", transactionId);
        redisTemplate.delete(otpKey);
        completeTransaction(transaction);
        return mapToResponse(transaction);
    }
    private void compensateTransaction(Transaction transaction, String reason) {
        log.warn("SAGA COMPENSATION - refunding: {} amount: {}",transaction.getSenderAccountNumber(),transaction.getAmount());
        //CREDIT MONEY BACK TO SENDER SYNCHRONOUSLY

        accountServiceClient.creditBalance(
                transaction.getSenderAccountNumber(),
                transaction.getAmount()
        );
        transaction.setStatus(TransactionStatus.FLAGGED);
        transaction.setFailureReason(reason + " - SAGA Compensation executed, amount refunded at " + Instant.now());
        transactionReposiotry.save(transaction);

        //PUBLISH refund event - Notification service will alert user
        Map<String,Object> refundEvent = new HashMap<>();
        refundEvent.put("amount",transaction.getAmount());
        refundEvent.put("reason",reason);
        refundEvent.put("transactionId",transaction.getId());
        refundEvent.put("senderAccountNumber",transaction.getSenderAccountNumber());

        kafkaTemplate.send(TRANSACTION_REFUNDED_TOPIC,transaction.getId(), refundEvent);
        log.info("SAGA COMPENSATION COMPLETE - {} refunded to {}",transaction.getAmount(),transaction.getSenderAccountNumber());


    }
    private void blockAccountAndCompensate(Transaction transaction, String reason) {
        //Publish fraud.detected -> Account Service will block account
        Map<String,String> fraudEvent  = new HashMap<>();
        fraudEvent.put("transactionId",transaction.getId());
        fraudEvent.put("accountNumber",transaction.getSenderAccountNumber());
        fraudEvent.put("reason",reason);

        kafkaTemplate.send(FRAUD_DETECTED_TOPIC,transaction.getSenderAccountNumber(),fraudEvent);
        log.warn("fraud.detected published - account: {} will be blocked, Kindly contact to the bank",transaction.getSenderAccountNumber());

        // SAGA COMPENSATION - refund Sender
        compensateTransaction(transaction,reason);

    }
    private void completeTransaction(Transaction transaction) {
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompleteAt(Instant.now());
        transactionReposiotry.save(transaction);

        TransactonCompletedEvent transactionCompletedEvent = new TransactonCompletedEvent();
        transactionCompletedEvent.builder()
                .transactionId(transaction.getId())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .senderAccountNumber(transaction.getSenderAccountNumber())
                .receiverAccountNumber(transaction.getReceiverAccountNumber())
                .build();
        log.info("SAGA COMPLETE - Transaction {} completed",transaction.getId());
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        return  response.builder()
                .id(transaction.getId())
                .senderAccountNumber(transaction.getSenderAccountNumber())
                .receiverAccountNumber(transaction.getReceiverAccountNumber())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .description(transaction.getDescription())
                .referenceNumber(transaction.getReferenceNumber())
                .creationAt(transaction.getCreationAt())
                .completeAt(transaction.getCompleteAt())
                .build();
    }



}
