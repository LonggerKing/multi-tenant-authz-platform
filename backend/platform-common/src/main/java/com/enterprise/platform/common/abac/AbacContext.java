package com.enterprise.platform.common.abac;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ABAC 求值上下文，作为 SpEL 表达式的 root。
 * <p>策略表达式示例：{@code deptId == 10}、{@code hour >= 9 && hour < 18}、
 * {@code hasRole('TENANT_ADMIN')}、{@code ip != null && ip.startsWith('10.')}</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbacContext implements Serializable {

    private Long userId;
    private String username;
    private Long tenantId;
    private String tenantCode;
    private Long deptId;

    @Builder.Default
    private List<String> roles = List.of();

    /** 客户端 IP。 */
    private String ip;

    /** 当前小时（0-23），用于时间窗策略。 */
    private int hour;

    /** 当前时间。 */
    private LocalDateTime now;

    /** 扩展属性（如请求体中的资源归属部门、单据金额等）。 */
    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();

    public boolean hasRole(String roleCode) {
        return roles != null && roles.contains(roleCode);
    }

    public boolean inDept(Long targetDeptId) {
        return deptId != null && deptId.equals(targetDeptId);
    }

    public Object attr(String key) {
        return attributes == null ? null : attributes.get(key);
    }

    public Object attr(String key, Object defaultValue) {
        Object value = attr(key);
        return value == null ? defaultValue : value;
    }
}
