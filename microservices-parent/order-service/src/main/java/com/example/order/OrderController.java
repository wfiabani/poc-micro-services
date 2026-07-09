package com.example.order;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final InventoryClient inventoryClient;
    private final ProductCatalog productCatalog;

    public OrderController(InventoryClient inventoryClient, ProductCatalog productCatalog) {
        this.inventoryClient = inventoryClient;
        this.productCatalog = productCatalog;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listOrders() {
        return ResponseEntity.ok(List.of(
                Map.of("id", 1, "product", "Widget A", "quantity", 10),
                Map.of("id", 2, "product", "Widget B", "quantity", 5)
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("id", id, "product", "Widget A", "quantity", 10));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, Object> order) {
        Long productId = ((Number) order.get("productId")).longValue();
        Optional<ProductCatalog.Product> product = productCatalog.findById(productId);
        if (product.isEmpty()) {
            Map<String, Object> notFound = Map.of(
                    "error", "Produto não encontrado",
                    "productId", productId
            );
            return ResponseEntity.status(404).body(notFound);
        }

        Map<String, Object> stockInfo = inventoryClient.checkStock(product.get().sku());
        Map<String, Object> body = Map.of(
                "order", order,
                "stockCheck", stockInfo
        );
        return ResponseEntity.status(201).body(body);
    }
}
