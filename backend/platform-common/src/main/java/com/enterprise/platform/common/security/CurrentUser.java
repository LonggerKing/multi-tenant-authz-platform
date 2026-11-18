package com.enterprise.platform.common.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 已认证主体（从令牌解析得到）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUser implements Serializable {

    private Long userId;
    private String username;
    private String nickname;
    private Long tenantId;
    private String tenantCode;
    private Long deptId;
    private boolean superAdmin;

    @Builder.Default
    private List<String> roles = new ArrayList<>();

    /** 令牌唯一标识，用于登出黑名单。 */
    private String tokenId;

    /** 令牌类型：access / refresh。 */
    private String tokenType;

    public boolean hasRole(String roleCode) {
        return roles != null && roles.contains(roleCode);
    }
}
