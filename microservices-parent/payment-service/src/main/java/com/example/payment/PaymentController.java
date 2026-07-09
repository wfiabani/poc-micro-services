package com.example.payment;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final OrderClient orderClient;

    public PaymentController(OrderClient orderClient) {
        this.orderClient = orderClient;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPayment(@PathVariable String id) {
        Map<String, Object> orderInfo = orderClient.getOrder(id);
        return ResponseEntity.ok(Map.of(
                "paymentId", id,
                "status", "APPROVED",
                "orderInfo", orderInfo
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createPayment(@RequestBody Map<String, Object> payment) {
        String orderId = (String) payment.getOrDefault("orderId", "UNKNOWN");
        Map<String, Object> orderInfo = orderClient.getOrder(orderId);
        return ResponseEntity.status(201).body(Map.of(
                "payment", payment,
                "orderInfo", orderInfo,
                "status", "APPROVED"
        ));
    }
}
