package com.example.notification;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final PaymentClient paymentClient;
    private final ShippingClient shippingClient;
    private final ExecutorService notificationExecutor;

    public NotificationController(PaymentClient paymentClient,
                                   ShippingClient shippingClient,
                                   ExecutorService notificationExecutor) {
        this.paymentClient = paymentClient;
        this.shippingClient = shippingClient;
        this.notificationExecutor = notificationExecutor;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createNotification(@RequestBody Map<String, Object> request) {
        String paymentId = (String) request.getOrDefault("paymentId", "UNKNOWN");
        String sku       = (String) request.getOrDefault("sku", "UNKNOWN");

        // Busca pagamento e envio em paralelo para reduzir a latência da notificação.
        CompletableFuture<Map<String, Object>> paymentFuture =
                CompletableFuture.supplyAsync(() -> paymentClient.getPayment(paymentId), notificationExecutor);
        CompletableFuture<Map<String, Object>> shippingFuture =
                CompletableFuture.supplyAsync(() -> shippingClient.getShipment(sku), notificationExecutor);

        CompletableFuture.allOf(paymentFuture, shippingFuture).join();

        return ResponseEntity.status(201).body(Map.of(
                "notification", request,
                "paymentStatus", paymentFuture.join(),
                "shippingStatus", shippingFuture.join()
        ));
    }
}
