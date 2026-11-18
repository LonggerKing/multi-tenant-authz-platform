package com.enterprise.platform.permission.cache;

import com.enterprise.platform.common.util.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 多级权限缓存：L1 Caffeine（进程内，微秒级） + L2 Redis（跨实例共享）。
 * <p>Redis 不可用时自动降级为单机缓存，不影响鉴权正确性。</p>
 */
@Slf4j
@Service
public class PermissionCacheService {

    private final Cache<String, Object> l1;
    private final RedisTemplate<String, String> redisTemplate;
    private final boolean redisEnabled;
    private final Duration ttl;
    private volatile boolean redisHealthy = true;

    public PermissionCacheService(RedisTemplate<String, String> redisTemplate,
                                  @Value("${platform.cache.redis-enabled:false}") boolean redisEnabled,
                                  @Value("${platform.cache.ttl-seconds:300}") long ttlSeconds,
                                  @Value("${platform.cache.local-max-size:20000}") long localMaxSize) {
        this.redisTemplate = redisTemplate;
        this.redisEnabled = redisEnabled;
        this.ttl = Duration.ofSeconds(Math.max(ttlSeconds, 10));
        this.l1 = Caffeine.newBuilder()
                .maximumSize(localMaxSize)
                .expireAfterWrite(Duration.ofSeconds(Math.min(ttlSeconds, 60)))
                .build();
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, TypeReference<T> type, Supplier<T> loader) {
        Object local = l1.getIfPresent(key);
        if (local != null) {
            return (T) local;
        }
        if (redisEnabled) {
            String json = redisGet(key);
            if (json != null) {
                try {
                    T value = JsonUtils.fromJson(json, type);
                    if (value != null) {
                        l1.put(key, value);
                        return value;
                    }
                } catch (Exception e) {
                    log.warn("[权限缓存] 反序列化失败，忽略 L2 命中: key={}", key);
                }
            }
        }
        T value = loader.get();
        if (value != null) {
            l1.put(key, value);
            if (redisEnabled) {
                redisSet(key, JsonUtils.toJson(value));
            }
        }
        return value;
    }

    public void put(String key, Object value) {
        l1.put(key, value);
        if (redisEnabled) {
            redisSet(key, JsonUtils.toJson(value));
        }
    }

    /**
     * 按键前缀失效（租户配置变更、角色授权变更时调用）。
     */
    public void evictByPrefix(String prefix) {
        l1.asMap().keySet().removeIf(key -> key.startsWith(prefix));
        if (!redisEnabled) {
            return;
        }
        List<String> keys = new ArrayList<>();
        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match(prefix + "*").count(500).build())) {
            cursor.forEachRemaining(keys::add);
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            markRedisUnhealthy(e);
        }
    }

    private String redisGet(String key) {
        if (!redisHealthy) {
            return null;
        }
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            markRedisUnhealthy(e);
            return null;
        }
    }

    private void redisSet(String key, String value) {
        if (!redisHealthy) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (Exception e) {
            markRedisUnhealthy(e);
        }
    }

    private void markRedisUnhealthy(Exception e) {
        if (redisHealthy) {
            log.warn("[权限缓存] Redis 不可用，降级为本地缓存: {}", e.getMessage());
        }
        redisHealthy = false;
    }
}
