package com.example.supplier;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class SupplierCatalog {

    public record SupplierStock(String sku, int supplierQuantity, boolean supplierAvailable, int leadTimeDays) {}

    private final Map<String, SupplierStock> stocks = Map.of(
            "WIDGET-A", new SupplierStock("WIDGET-A", 500, true, 3),
            "WIDGET-B", new SupplierStock("WIDGET-B", 50, true, 7),
            "WIDGET-C", new SupplierStock("WIDGET-C", 0, false, 15)
    );

    public Optional<SupplierStock> findBySku(String sku) {
        return Optional.ofNullable(stocks.get(sku));
    }
}
