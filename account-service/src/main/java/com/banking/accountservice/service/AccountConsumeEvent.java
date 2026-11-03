package com.banking.accountservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;

import java.math.BigDecimal;
import java.util.Map;
@Slf4j
@RequiredArgsConstructor
public class AccountConsumeEvent
{
    private final AccountService accountService;
    /*
        Consume transaction.completed event from kafka
        credits receiver account
        @param payload
    * */
    @KafkaListener(topics="transaction.completed")
    public void consumeTransactionCompleted(
            @Payload Map<String,Object> payload
            )
    {
        try{
            String receiverAccountNumber = (String) payload.get("receiverAccountNumber");
            BigDecimal amount = (BigDecimal) payload.get("amount");
            log.info("Crediting account: {} amount: {}", receiverAccountNumber, amount);
            accountService.creditBalance(receiverAccountNumber, amount);

        }catch(Exception e){
            log.error("Error crediting account",e.getMessage());
        }

    }
    /*
      Consume fraud detected event from kafka
      Blocks the flagged account
      @param paylod
    * */
    @KafkaListener(topics="fraud.detechted")
    public void consumeFraudetected(
            @Payload Map<String,Object> payload
    ){
        try{
            String receiverAccountNumber = (String) payload.get("receiverAccountNumber");
            log.info("Fraud detected: {} amount: {}", receiverAccountNumber, payload.get("amount"));
            accountService.blockAccount(receiverAccountNumber);
        }catch(Exception e){
            log.error("Error crediting account",e.getMessage());
        }
    }

}
