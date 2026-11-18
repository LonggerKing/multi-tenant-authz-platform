package com.enterprise.platform.tenant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 多租户相关配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "platform.tenant")
public class TenantProperties {

    /** 是否开启租户上下文解析。 */
    private boolean enabled = true;

    /** 是否允许通过请求头切换租户（生产环境仅建议在网关可信场景开启）。 */
    private boolean headerResolveEnabled = true;

    /** 不参与 tenant_id 过滤的表（全局表）。 */
    private List<String> ignoreTables = List.of(
            "sys_tenant", "sys_login_log_global", "information_schema", "flyway_schema_history");

    /** 独立 Schema 名称前缀。 */
    private String schemaPrefix = "tenant_";

    /** 是否允许创建独立 Schema 租户（H2 等嵌入式库可关闭）。 */
    private boolean schemaIsolationEnabled = true;

    /** 租户信息本地缓存秒数。 */
    private long cacheSeconds = 300;
}
