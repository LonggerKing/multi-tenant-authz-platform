package com.enterprise.platform.tenant.service;

import com.enterprise.platform.common.tenant.TenantInfo;
import com.enterprise.platform.tenant.config.TenantProperties;
import com.enterprise.platform.tenant.entity.SysTenant;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 租户元数据本地缓存：租户解析发生在每个请求的最前端，必须避免每次访问数据库。
 */
@Component
public class TenantCache {

    private final Cache<String, TenantInfo> byCode;
    private final Cache<Long, TenantInfo> byId;

    public TenantCache(TenantProperties properties) {
        Duration ttl = Duration.ofSeconds(Math.max(properties.getCacheSeconds(), 10));
        this.byCode = Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(10_000).build();
        this.byId = Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(10_000).build();
    }

    public TenantInfo getByCode(String code) {
        return byCode.getIfPresent(code);
    }

    public TenantInfo getById(Long id) {
        return byId.getIfPresent(id);
    }

    public void put(SysTenant tenant) {
        TenantInfo info = toInfo(tenant);
        byCode.put(info.getCode(), info);
        byId.put(info.getId(), info);
    }

    public void evict(String code) {
        if (code != null) {
            byCode.invalidate(code);
        }
        byId.invalidateAll();
    }

    public void evictAll() {
        byCode.invalidateAll();
        byId.invalidateAll();
    }

    public static TenantInfo toInfo(SysTenant tenant) {
        return TenantInfo.builder()
                .id(tenant.getId())
                .code(tenant.getCode())
                .name(tenant.getName())
                .isolationMode(tenant.getIsolationMode())
                .schemaName(tenant.getSchemaName())
                .build();
    }
}
