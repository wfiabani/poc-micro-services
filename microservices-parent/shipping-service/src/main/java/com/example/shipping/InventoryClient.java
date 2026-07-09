package com.example.shipping;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

@Component
public class InventoryClient {

    private final RestClient restClient;

    public InventoryClient(
            RestClient.Builder builder,
            @Value("${inventory.service.url}") String baseUrl) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(2));

        this.restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    @CircuitBreaker(name = "inventory", fallbackMethod = "fallback")
    public Map<String, Object> checkStock(String sku) {
        return restClient.get()
                .uri("/inventory/{sku}", sku)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    Map<String, Object> fallback(String sku, Throwable ex) {
        return Map.of(
                "sku", sku,
                "available", false,
                "message", "Estoque indisponível no momento"
        );
    }
}
