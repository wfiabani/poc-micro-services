package com.example.inventory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final SupplierClient supplierClient;
    private final InventoryCatalog inventoryCatalog;

    public InventoryController(SupplierClient supplierClient, InventoryCatalog inventoryCatalog) {
        this.supplierClient = supplierClient;
        this.inventoryCatalog = inventoryCatalog;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listItems() {
        return ResponseEntity.ok(List.of(
                Map.of("sku", "WIDGET-A", "quantity", 100, "available", true),
                Map.of("sku", "WIDGET-B", "quantity", 0,   "available", false)
        ));
    }

    @GetMapping("/{sku}")
    public ResponseEntity<Map<String, Object>> getItem(@PathVariable String sku) {
        Optional<InventoryCatalog.InventoryItem> item = inventoryCatalog.findBySku(sku);
        if (item.isEmpty()) {
            Map<String, Object> notFound = Map.of(
                    "error", "SKU não encontrado",
                    "sku", sku
            );
            return ResponseEntity.status(404).body(notFound);
        }

        Map<String, Object> supplierStock = supplierClient.getSupplierStock(sku);
        Map<String, Object> body = Map.of(
                "sku", item.get().sku(),
                "quantity", item.get().quantity(),
                "available", item.get().available(),
                "supplierInfo", supplierStock
        );
        return ResponseEntity.ok(body);
    }

    @PutMapping("/{sku}")
    public ResponseEntity<Map<String, Object>> updateStock(
            @PathVariable String sku,
            @RequestBody Map<String, Object> update) {
        return ResponseEntity.ok(Map.of("sku", sku, "updated", true));
    }
}
