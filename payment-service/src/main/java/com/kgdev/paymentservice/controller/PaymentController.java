package com.kgdev.paymentservice.controller;

import com.kgdev.paymentservice.dto.CreatePaymentRequest;
import com.kgdev.paymentservice.dto.PaymentOrderResponse;
import com.kgdev.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    @PostMapping
    public ResponseEntity<PaymentOrderResponse> save(@Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.created(
                paymentService.createPaymentOrder(request)
        );
    }
    @PostMapping("/webhooks")
    public ResponseEntity<String> handleWebhook(
            @RequestBody Map<String,Object> payload
    ){
        paymentService.handleWebhook(payload);
        return ResponseEntity.ok("Webhook processed");
    }
}
