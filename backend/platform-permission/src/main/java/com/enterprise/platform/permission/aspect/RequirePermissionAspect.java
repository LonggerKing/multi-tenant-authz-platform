package com.enterprise.platform.permission.aspect;

import com.enterprise.platform.common.annotation.RequirePermission;
import com.enterprise.platform.common.annotation.RequireSuperAdmin;
import com.enterprise.platform.permission.service.PermissionChecker;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * 声明式权限注解切面，与 {@code @PreAuthorize} 并存，供无法使用 SpEL 的场景（如内部服务）复用。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RequirePermissionAspect {

    private final PermissionChecker permissionChecker;

    @Before("@annotation(requirePermission)")
    public void checkPermission(JoinPoint joinPoint, RequirePermission requirePermission) {
        String[] codes = requirePermission.value();
        boolean allowed = requirePermission.requireAll()
                ? permissionChecker.hasAll(codes)
                : permissionChecker.hasAny(codes);
        if (!allowed) {
            throw new AccessDeniedException("缺少权限: " + String.join(",", codes));
        }
    }

    @Before("@annotation(requireSuperAdmin)")
    public void checkSuperAdmin(JoinPoint joinPoint, RequireSuperAdmin requireSuperAdmin) {
        if (!permissionChecker.superAdmin()) {
            throw new AccessDeniedException("仅平台超管可访问");
        }
    }
}
