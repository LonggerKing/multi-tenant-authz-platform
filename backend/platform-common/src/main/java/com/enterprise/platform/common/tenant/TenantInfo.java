package com.enterprise.platform.common.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 当前请求命中的租户快照（由租户过滤器解析后写入上下文）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantInfo implements Serializable {

    private Long id;
    private String code;
    private String name;
    private IsolationMode isolationMode;
    private String schemaName;

    public boolean isSchemaIsolated() {
        return isolationMode == IsolationMode.SCHEMA && schemaName != null && !schemaName.isBlank();
    }
}
