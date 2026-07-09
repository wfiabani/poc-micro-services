package com.example.shipping;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/shipments")
public class ShippingController {

    private final InventoryClient inventoryClient;

    public ShippingController(InventoryClient inventoryClient) {
        this.inventoryClient = inventoryClient;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getShipment(@PathVariable String id) {
        Map<String, Object> inventoryInfo = inventoryClient.checkStock(id);
        return ResponseEntity.ok(Map.of(
                "shipmentId", id,
                "status", "IN_TRANSIT",
                "inventoryInfo", inventoryInfo
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createShipment(@RequestBody Map<String, Object> shipment) {
        String sku = (String) shipment.getOrDefault("sku", "UNKNOWN");
        Map<String, Object> inventoryInfo = inventoryClient.checkStock(sku);
        return ResponseEntity.status(201).body(Map.of(
                "shipment", shipment,
                "inventoryInfo", inventoryInfo,
                "status", "SCHEDULED"
        ));
    }
}
