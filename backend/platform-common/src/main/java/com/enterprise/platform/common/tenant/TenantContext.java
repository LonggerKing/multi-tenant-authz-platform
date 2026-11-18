package com.enterprise.platform.common.tenant;

import com.enterprise.platform.common.exception.BizException;
import com.enterprise.platform.common.exception.ErrorCode;

/**
 * 租户上下文。请求进来由过滤器写入，请求结束必须清理，避免线程池串租户。
 */
public final class TenantContext {

    private static final ThreadLocal<TenantInfo> HOLDER = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(TenantInfo tenant) {
        HOLDER.set(tenant);
    }

    public static TenantInfo get() {
        return HOLDER.get();
    }

    public static boolean isPresent() {
        return HOLDER.get() != null;
    }

    public static TenantInfo require() {
        TenantInfo tenant = HOLDER.get();
        if (tenant == null) {
            throw BizException.of(ErrorCode.TENANT_MISSING);
        }
        return tenant;
    }

    public static Long currentTenantId() {
        return require().getId();
    }

    public static String currentTenantCode() {
        return require().getCode();
    }

    /**
     * 独立 Schema 模式返回租户 schema，共享表模式返回 null。
     */
    public static String currentSchema() {
        TenantInfo tenant = HOLDER.get();
        return tenant != null && tenant.isSchemaIsolated() ? tenant.getSchemaName() : null;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
