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
public class ShippingClient {

    private final RestClient restClient;

    public ShippingClient(
            RestClient.Builder builder,
            @Value("${shipping.service.url}") String baseUrl) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(2));

        this.restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    @CircuitBreaker(name = "shipping", fallbackMethod = "fallback")
    public Map<String, Object> getShipment(String sku) {
        return restClient.get()
                .uri("/shipments/{sku}", sku)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    Map<String, Object> fallback(String sku, Throwable ex) {
        return Map.of(
                "sku", sku,
                "status", "UNKNOWN",
                "message", "Serviço de envio indisponível no momento"
        );
    }
}
