package com.enterprise.platform.common.constant;

/**
 * 平台通用常量。
 */
public final class PlatformConstants {

    private PlatformConstants() {
    }

    /** 租户标识请求头（值为租户编码或租户 ID）。 */
    public static final String HEADER_TENANT = "X-Tenant-Id";

    /** 请求链路标识，便于日志串联。 */
    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    /** Casbin 主体前缀，避免与角色主体混淆。 */
    public static final String SUBJECT_USER_PREFIX = "u:";

    /** 平台超管角色编码（跨租户运维）。 */
    public static final String PLATFORM_ADMIN_ROLE = "PLATFORM_ADMIN";

    /** 租户内置管理员角色编码。 */
    public static final String TENANT_ADMIN_ROLE = "TENANT_ADMIN";

    /** 所有资源通配符。 */
    public static final String WILDCARD = "*";

    /** 缓存 key 命名空间。 */
    public static final String CACHE_NS = "perm:";
}
