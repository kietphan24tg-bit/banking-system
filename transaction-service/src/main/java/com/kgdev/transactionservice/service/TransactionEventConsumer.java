package com.kgdev.transactionservice.service;

import com.kgdev.transactionservice.entity.Transaction;
import com.kgdev.transactionservice.entity.TransactionStatus;
import com.kgdev.transactionservice.repository.TransactionReposiotry;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.KafkaListeners;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionEventConsumer {
    private final TransactionReposiotry transactionReposiotry;
    private final RedisTemplate<String,String> redisTemplate;
    private final KafkaTemplate<String,Object> kafkaTemplate;
    private final TransactionService transactionService;
    private static int OTP_EXPIRY_MINUTES = 5;
    private static final String TRANSACTION_OTP_GENERATED_TOPIC = "transaction.otp.generated";
    /*
    consume verification.required
    Generate OTP and ask user to verify
    @Param payload
     */
    @Transactional
    public void consumeVerificationRequired(
            @Payload Map<String,Object> payload
            ){
        try{
            String transactionId = (String) payload.get("transactionId");
            String accountNumber = (String) payload.get("accountNumber");
            String reason = (String) payload.get("reason");
            log.info("Verification required - transaction: {} reason: {}",transactionId,reason);
            Transaction transaction = transactionReposiotry.findById(transactionId)
                    .orElseThrow(()->new RuntimeException("Transaction not found"));
            if(transaction.getStatus() != TransactionStatus.PROCESSING){
                log.warn("Transaction {} not PROCESSING - skipping",transactionId);
                return;
            }
            //Generate 6 digit otp
            String otp = String.format("%06d",(int)(Math.random()*900000) + 100000);

            //Store OTP in redis - expire in 5 minutes
            String otpKey = "verification:otp" + transactionId;
            redisTemplate.opsForValue().set(otpKey,otp,OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);

            //Update status of transaction
            transaction.setStatus(TransactionStatus.PENDING_VERIFICATION);
            transactionReposiotry.save(transaction);
            log.info("OTP generated for transaction: {} expires in {} min",transactionId,OTP_EXPIRY_MINUTES);

            //Notification
            Map<String,Object> otpEvent = new HashMap<>();
            otpEvent.put("transactionId",transactionId);
            otpEvent.put("accountNumber",accountNumber);
            otpEvent.put("reason",reason);
            otpEvent.put("otp",otp);
            otpEvent.put("amount",payload.get("amount"));
            kafkaTemplate.send(TRANSACTION_OTP_GENERATED_TOPIC,transactionId,otpEvent);

         }catch(Exception e){
            log.error("Error handling verification required {}",e.getMessage(),e);
        }
    }
    private void processCleanResult(String transactionId){
        Transaction transaction = transactionReposiotry.findById(transactionId).orElseThrow(()->new RuntimeException("Transaction not found"));
        if(transaction.getStatus() != TransactionStatus.PROCESSING){
            log.warn("Transaction {} not PROCESSING - skipping",transactionId);
            return;
        }
        completeTransaction(transaction);
    }
    @KafkaListener(topics = "fraud.check.clean")
    public void consumeFraudCheckCleanResult(@Payload Map<String,Object> payload){
        try{
            String transactionId = (String) payload.get("transactionId");
            transactionService.processCleanResult(transactionId);
        }catch(Exception e){

        }
    }
}
