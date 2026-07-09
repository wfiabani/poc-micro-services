package com.example.order;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class ProductCatalog {

    public record Product(Long id, String name, String sku) {}

    private final Map<Long, Product> products = Map.of(
            1L, new Product(1L, "Widget A", "WIDGET-A"),
            2L, new Product(2L, "Widget B", "WIDGET-B"),
            3L, new Product(3L, "Widget C", "WIDGET-C")
    );

    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(products.get(id));
    }
}
