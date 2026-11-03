package com.kgdev.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentOrderResponse {
    String paymentId;
    String razorpayOrderId;
    BigDecimal amount;
    String currency;
    String status;
    String razorpayKeyId;
}
