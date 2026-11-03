package com.kgdev.frauddetectionservice.service;

import com.kgdev.frauddetectionservice.client.AccountServiceClient;
import com.kgdev.frauddetectionservice.model.FraudCheckResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDectectionService {
    private final AccountServiceClient accountServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String VERIFICATION_REQUIRED_TOPIC="verification.required";
    private final String FRAUD_CHECK_CLEAN_RESULT_TOPIC ="fraud.check.clean";
    private final RedisTemplate<String, String> redisTemplate;

    @Value("{fraud.max-transactions-per-minute}")
    Integer maxTransactionsPerMinute;

    @Value("{fraud.suspicious.amount-multiplier}")
    double suspiciousAmountMultiplier;

    public void checkTransaction(Map<String, Object> payload) {
        String transactionId = (String) payload.get("transactionId");
        String accountNumber =  (String) payload.get("accountNumber");
        String amount = (String) payload.get("amount");

        //Fetch real balance from Account Service
        BigDecimal senderBalance = accountServiceClient.getBalance(accountNumber);
        log.info("Checking transaction: {} account : amount {} balance : {}", transactionId, amount, senderBalance);
        FraudCheckResult result = performFraudChecks(accountNumber,amount,senderBalance);
        if(result.isFraud()){
            log.info("Suspicious activity detected - account: {} "+ "reason: {} - request OTP verification",accountNumber,result.getReason());
            Map<String,Object> verfication = new HashMap<>();
            verfication.put("transactionId",transactionId);
            verfication.put("accountNumber",accountNumber);
            verfication.put("amount",amount);
            verfication.put("reason",result.getReason());
            kafkaTemplate.send(VERIFICATION_REQUIRED_TOPIC,transactionId,verfication);

        }else{
            log.info("Transaction clean");
            Map<String,Object> transactionCleanEvent = new HashMap<>();
            transactionCleanEvent.put("transactionId",transactionId);
            transactionCleanEvent.put("isFraud",false);
            transactionCleanEvent.put("reason",null);
            kafkaTemplate.send(FRAUD_CHECK_CLEAN_RESULT_TOPIC,transactionId,transactionCleanEvent);



        }




    }
    private FraudCheckResult performFraudChecks(
            String accountNumber,
            BigDecimal amount,
            BigDecimal senderBalance
    ){
        // Pattern 1: Velocity Check
        if(isVelocityExceeded(accountNumber)){
            return new FraudCheckResult(
                    true,
                    "Too many transactions in 60 seconds" + " - Velocity limit exceeded"
            );

        }

        //Pattern 2: Amount check
        if(isAmountSuspicious(accountNumber,amount)){
            return new FraudCheckResult(
                    true,
                    "Unual transaction amount" + "exceeds 3x your average"
            );

        }
        //Pattern 3: Balance check
        if(senderBalance.compareTo(BigDecimal.ZERO)>0 && isBalanceCheckFailed(senderBalance,amount)){
            return new FraudCheckResult(
                    true,
                    "Transaction exceed 90% of account balance"
            );
        }
    }
    private boolean isVelocityExceeded(String accountNumber

    ){
        String key ="fraud:velocity" + accountNumber;
        Long count = redisTemplate.opsForValue().increment(key);
        if(count!= null && count ==1){
            redisTemplate.expire(key, 60, TimeUnit.SECONDS);
        }

        log.info("Velocity check - account : {} count: {}/{} ", accountNumber,count,maxTransactionsPerMinute);
        return count!=null && count  > maxTransactionsPerMinute;
    }
    private boolean isAmountSuspicious(String  accountNumber, BigDecimal amount){
        String avgKey = "fraud:avg_amount" + accountNumber;
        String avgStr = redisTemplate.opsForValue().get(avgKey);
        if(avgStr==null){
            redisTemplate.opsForValue().set(avgKey,amount.toString());
            return false;
        }
        BigDecimal avgAmount = new BigDecimal(avgStr);
        BigDecimal threshold = avgAmount.multiply(
                BigDecimal.valueOf(suspiciousAmountMultiplier)
        );

        // update running average
        BigDecimal newAvg = avgAmount.add(amount).
                divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        redisTemplate.opsForValue().set(avgKey,newAvg.toString());

        log.info("Amount check - amount: {} threshold: {} suspicious: {} ",amount,threshold,amount.compareTo(threshold ) > 0);
        return amount.compareTo(threshold) >0;
    }
    boolean isBalanceCheckFailed(BigDecimal senderBalance,BigDecimal amount){
        BigDecimal maxAllowed = senderBalance.multiply(
          BigDecimal.valueOf(maxBalancePercentage)
        );
        log.info("Balance check - amount: {} maxAllowed: {} suscpious: {}",
                amount,maxAllowed,amount.compareTo(maxAllowed)> 0);
        return amount.compareTo(maxAllowed) > 0;
    }
}
