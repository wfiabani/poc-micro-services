package com.example.payment;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

@Component
public class OrderClient {

    private final RestClient restClient;

    public OrderClient(
            RestClient.Builder builder,
            @Value("${order.service.url}") String baseUrl) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(2));

        this.restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    @CircuitBreaker(name = "order", fallbackMethod = "fallback")
    public Map<String, Object> getOrder(String orderId) {
        return restClient.get()
                .uri("/orders/{id}", orderId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    Map<String, Object> fallback(String orderId, Throwable ex) {
        return Map.of(
                "orderId", orderId,
                "status", "UNKNOWN",
                "message", "Serviço de pedidos indisponível no momento"
        );
    }
}
