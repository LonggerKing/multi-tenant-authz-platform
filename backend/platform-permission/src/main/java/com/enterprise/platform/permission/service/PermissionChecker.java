package com.enterprise.platform.permission.service;

import com.enterprise.platform.common.exception.BizException;
import com.enterprise.platform.common.exception.ErrorCode;
import com.enterprise.platform.common.security.UserContext;
import com.enterprise.platform.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 暴露给 SpEL / AOP 的权限判定器，Bean 名称固定为 perm。
 * <p>用法：{@code @PreAuthorize("@perm.has('system:user:list')")}</p>
 */
@Component("perm")
@RequiredArgsConstructor
public class PermissionChecker {

    private final AuthorizationService authorizationService;

    public boolean has(String code) {
        return hasAny(code);
    }

    public boolean hasAny(String... codes) {
        if (codes == null || codes.length == 0) {
            return true;
        }
        if (!UserContext.isAuthenticated()) {
            return false;
        }
        if (UserContext.isSuperAdmin()) {
            return true;
        }
        String tenantCode = TenantContext.isPresent()
                ? TenantContext.currentTenantCode()
                : UserContext.require().getTenantCode();
        return Arrays.stream(codes)
                .anyMatch(code -> authorizationService.isAllowed(UserContext.require(), tenantCode, code, "*"));
    }

    public boolean hasAll(String... codes) {
        if (codes == null || codes.length == 0) {
            return true;
        }
        if (!UserContext.isAuthenticated()) {
            return false;
        }
        if (UserContext.isSuperAdmin()) {
            return true;
        }
        String tenantCode = TenantContext.isPresent()
                ? TenantContext.currentTenantCode()
                : UserContext.require().getTenantCode();
        return Arrays.stream(codes)
                .allMatch(code -> authorizationService.isAllowed(UserContext.require(), tenantCode, code, "*"));
    }

    public boolean superAdmin() {
        if (!UserContext.isAuthenticated()) {
            throw BizException.of(ErrorCode.UNAUTHORIZED);
        }
        return UserContext.isSuperAdmin();
    }
}
