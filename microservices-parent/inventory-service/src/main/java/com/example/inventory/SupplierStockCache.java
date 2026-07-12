package com.example.inventory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Cache distribuído (Redis) das consultas de estoque ao supplier-service,
 * compartilhado entre todas as réplicas do inventory-service.
 */
@Component
public class SupplierStockCache {

    private static final Logger log = LoggerFactory.getLogger(SupplierStockCache.class);
    private static final String KEY_PREFIX = "supplier-stock:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final Duration ttl;

    public SupplierStockCache(RedisTemplate<String, Object> redisTemplate,
                               @Value("${supplier.stock-cache.ttl-seconds:300}") long ttlSeconds) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getOrFetch(String sku, Supplier<Map<String, Object>> fetcher) {
        String key = KEY_PREFIX + sku;

        try {
            Map<String, Object> cached = (Map<String, Object>) redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return cached;
            }
        } catch (Exception ex) {
            log.warn("Falha ao ler cache no Redis para {}, seguindo sem cache", key, ex);
        }

        Map<String, Object> value = fetcher.get();

        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (Exception ex) {
            log.warn("Falha ao escrever cache no Redis para {}", key, ex);
        }

        return value;
    }
}
