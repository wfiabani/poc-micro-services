package com.example.supplier;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/suppliers")
public class SupplierController {

    private final SupplierCatalog supplierCatalog;

    public SupplierController(SupplierCatalog supplierCatalog) {
        this.supplierCatalog = supplierCatalog;
    }

    @GetMapping("/stock/{sku}")
    public ResponseEntity<Map<String, Object>> getStock(@PathVariable String sku) {
        Optional<SupplierCatalog.SupplierStock> stock = supplierCatalog.findBySku(sku);
        if (stock.isEmpty()) {
            Map<String, Object> notFound = Map.of(
                    "error", "SKU não encontrado",
                    "sku", sku
            );
            return ResponseEntity.status(404).body(notFound);
        }

        Map<String, Object> body = Map.of(
                "sku", stock.get().sku(),
                "supplierQuantity", stock.get().supplierQuantity(),
                "supplierAvailable", stock.get().supplierAvailable(),
                "leadTimeDays", stock.get().leadTimeDays()
        );
        return ResponseEntity.ok(body);
    }

    @PutMapping("/stock/{sku}")
    public ResponseEntity<Map<String, Object>> updateStock(
            @PathVariable String sku,
            @RequestBody Map<String, Object> update) {
        return ResponseEntity.ok(Map.of("sku", sku, "updated", true));
    }
}
