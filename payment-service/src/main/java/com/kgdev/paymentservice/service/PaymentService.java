package com.kgdev.paymentservice.service;

import com.kgdev.paymentservice.dto.CreatePaymentRequest;
import com.kgdev.paymentservice.dto.PaymentOrderResponse;
import com.kgdev.paymentservice.entity.Payment;
import com.kgdev.paymentservice.entity.PaymentStatus;
import com.kgdev.paymentservice.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final String PAYMENT_COMPLETED_TOPIC = "payment.completed";
    private final String PAYMENT_FAILED_TOPIC = "payment.failed";
    private final KafkaTemplate<String, Object> kafkaTemplate;
    @Value("${razorpay.key-id}")
    private String keyId;
    @Value("${razorpay.key-secret}")
    private String keySecret;
    /*
    Create Razopay payment order
    Flow:
    1. Create order in razorpay
    2. Save Payment record in DB
    3. Return order details to frontend
    4. Frontend show Razorpay checkout
    5. User pays
    6. Razorpay calls webhook
    * */
    public PaymentOrderResponse createPaymentOrder(
            CreatePaymentRequest request
    ) throws RazorpayException {
        log.info("Creating payment order for account: {} amount: {}", request.getAccountNumber(),request.getAmount());
        RazorpayClient razorpayClient = new RazorpayClient(keyId, keySecret);

        //Converted Amount
        int convertedAmount = request.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .intValue();

        JSONObject orderRequest = new JSONObject();

        int amountInCents = request.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .intValue();

        orderRequest.put("amount", amountInCents);
        orderRequest.put("currency", "USD");

        orderRequest.put(
                "receipt",
                "rcpt_" +
                        System.currentTimeMillis() +
                        UUID.randomUUID()
                                .toString()
                                .replace("-", "")
                                .substring(0, 10)
        );
        Order razorpayOrder = razorpayClient.orders.create(orderRequest);
        log.info("Razorpay order created: {}", razorpayOrder.get("id").toString());
        Payment payment = new Payment();
        payment.builder()
                .razorpayPaymentId(razorpayOrder.get("id"))
                .accountNumber(request.getAccountNumber())
                .amount(request.getAmount())
                .currency("USD")
                .status(PaymentStatus.CREATED)
                .description(request.getDescription())
                .build();


        Payment savedPayment = paymentRepository.save(payment);

        return new  PaymentOrderResponse(
                savedPayment.getId(),
                razorpayOrder.get("id").toString(),
                request.getAmount(),
                "USD",
                "CREATED",
                keyId
        );
    }
    public void handleWebhook(Map<String, Object> payload) {
        log.info("Received Razorpay webhook: {}", payload.get("event"));
        String event = payload.get("event").toString();
        if("payment.captured".equals(event)) {
            handlePaymentSuccess(payload);
        }else if("payment.failed".equals(event)) {
            handlePaymentFailure(event);
        }
    }
    public void handlePaymentSuccess(Map<String, Object> payload) {
        try{
           Map<String,Object> paymentData = extractPaymentData(payload);
           String orderId = paymentData.get("order_id").toString();
           String paymentId = paymentData.get("id").toString();
           Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
                   .orElseThrow(()->new RuntimeException("Payment not found"));

           payment.setRazorpayPaymentId(paymentId);
           payment.setStatus(PaymentStatus.COMPLETED);
           paymentRepository.save(payment);
           //Publish payment completed event
            Map<String,Object> event =  new HashMap<>();
            event.put("paymentId", paymentId);
            event.put("accountNumber", payment.getAccountNumber());
            event.put("amount", payment.getAmount());
            event.put("razorpayPaymentId", payment.getRazorpayPaymentId());
            kafkaTemplate.send(PAYMENT_COMPLETED_TOPIC, paymentId, event);
            log.info("Payment completed: {}", paymentId);
        }catch(Exception e){
            log.error("Error handling payment success: {}", e.getMessage());
        }

    }
    public void handlePaymentFailure(Map<String,Object> payload) {
        try{
            Map<String,Object> paymentData = extractPaymentData(payload);
            String orderId = paymentData.get("order_id").toString();
            String paymentId = paymentData.get("id").toString();
            Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
                    .orElseThrow(()->new RuntimeException("Payment not found"));

            payment.setRazorpayPaymentId(paymentId);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment failed via Razorpay");
            paymentRepository.save(payment);

            //Publish payment completed event
            Map<String,Object> event =  new HashMap<>();
            event.put("paymentId", paymentId);
            event.put("accountNumber", payment.getAccountNumber());
            event.put("amount", payment.getAmount());
            event.put("razorpayPaymentId", payment.getRazorpayPaymentId());
            event.put("reason","Payment failed via Razorpay");
            kafkaTemplate.send(PAYMENT_FAILED_TOPIC, paymentId, event);
            log.info("Payment completed: {}", paymentId);
        }catch(Exception e){
            log.error("Error handling payment failed: {}", e.getMessage());
        }
    }
    private Map<String,Object> extractPaymentData(Map<String, Object> payload) {
        Map<String,Object> entity  = (Map<String,Object>)payload.get("payload");
        Map<String,Object> paymentWrapper = (Map<String,Object>)entity.get("payment");
        return (Map<String,Object>)paymentWrapper.get("entity");
    }
}
