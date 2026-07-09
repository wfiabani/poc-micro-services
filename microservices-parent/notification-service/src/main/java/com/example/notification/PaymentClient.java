package com.example.notification;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

@Component
public class PaymentClient {

    private final RestClient restClient;

    public PaymentClient(
            RestClient.Builder builder,
            @Value("${payment.service.url}") String baseUrl) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(2));

        this.restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    @CircuitBreaker(name = "payment", fallbackMethod = "fallback")
    public Map<String, Object> getPayment(String paymentId) {
        return restClient.get()
                .uri("/payments/{id}", paymentId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    Map<String, Object> fallback(String paymentId, Throwable ex) {
        return Map.of(
                "paymentId", paymentId,
                "status", "UNKNOWN",
                "message", "Serviço de pagamento indisponível no momento"
        );
    }
}
