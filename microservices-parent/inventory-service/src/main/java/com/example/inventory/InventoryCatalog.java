package com.example.inventory;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class InventoryCatalog {

    public record InventoryItem(String sku, int quantity, boolean available) {}

    private final Map<String, InventoryItem> items = Map.of(
            "WIDGET-A", new InventoryItem("WIDGET-A", 100, true),
            "WIDGET-B", new InventoryItem("WIDGET-B", 0, false),
            "WIDGET-C", new InventoryItem("WIDGET-C", 15, true)
    );

    public Optional<InventoryItem> findBySku(String sku) {
        return Optional.ofNullable(items.get(sku));
    }
}
