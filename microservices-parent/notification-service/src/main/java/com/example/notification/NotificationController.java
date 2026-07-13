package com.example.notification;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

        // Busca pagamento e envio em paralelo para reduzir a latência da notificação.
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Map<String, Object>> paymentFuture  = executor.submit(() -> paymentClient.getPayment(paymentId));
        Future<Map<String, Object>> shippingFuture = executor.submit(() -> shippingClient.getShipment(sku));

        Map<String, Object> paymentInfo;
        Map<String, Object> shippingInfo;
        try {
            paymentInfo  = paymentFuture.get();
            shippingInfo = shippingFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.status(201).body(Map.of(
                "notification", request,
                "paymentStatus", paymentInfo,
                "shippingStatus", shippingInfo
        ));
    }
}
