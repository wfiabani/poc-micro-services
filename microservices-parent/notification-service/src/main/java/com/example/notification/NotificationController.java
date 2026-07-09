package com.example.notification;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final PaymentClient paymentClient;
    private final ShippingClient shippingClient;

    public NotificationController(PaymentClient paymentClient, ShippingClient shippingClient) {
        this.paymentClient = paymentClient;
        this.shippingClient = shippingClient;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createNotification(@RequestBody Map<String, Object> request) {
        String paymentId = (String) request.getOrDefault("paymentId", "UNKNOWN");
        String sku       = (String) request.getOrDefault("sku", "UNKNOWN");

        Map<String, Object> paymentInfo  = paymentClient.getPayment(paymentId);
        Map<String, Object> shippingInfo = shippingClient.getShipment(sku);

        return ResponseEntity.status(201).body(Map.of(
                "notification", request,
                "paymentStatus", paymentInfo,
                "shippingStatus", shippingInfo
        ));
    }
}
