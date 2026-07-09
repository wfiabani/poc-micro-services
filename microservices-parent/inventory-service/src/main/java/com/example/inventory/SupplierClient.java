package com.example.inventory;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

@Component
public class SupplierClient {

    private final RestClient restClient;

    public SupplierClient(
            RestClient.Builder builder,
            @Value("${supplier.service.url}") String baseUrl) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(2));

        this.restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    @CircuitBreaker(name = "supplier", fallbackMethod = "fallback")
    public Map<String, Object> getSupplierStock(String sku) {
        return restClient.get()
                .uri("/suppliers/stock/{sku}", sku)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    Map<String, Object> fallback(String sku, Throwable ex) {
        return Map.of(
                "sku", sku,
                "supplierAvailable", false,
                "message", "Fornecedor indisponível no momento"
        );
    }
}
