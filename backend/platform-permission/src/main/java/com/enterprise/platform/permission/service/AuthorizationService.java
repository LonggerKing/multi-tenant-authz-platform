package com.enterprise.platform.permission.service;

import com.enterprise.platform.common.abac.AbacContext;
import com.enterprise.platform.common.constant.PlatformConstants;
import com.enterprise.platform.common.security.CurrentUser;
import com.enterprise.platform.common.security.UserContext;
import com.enterprise.platform.common.tenant.TenantContext;
import com.enterprise.platform.common.util.WebUtils;
import com.enterprise.platform.permission.abac.ApiPolicyService;
import com.enterprise.platform.permission.casbin.CasbinEnforcerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;

/**
 * 统一鉴权入口：RBAC（Casbin）+ ABAC（属性条件）叠加判定。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final CasbinEnforcerService casbinEnforcerService;
    private final ApiPolicyService apiPolicyService;

    /**
     * 判断当前登录用户是否具备某权限点/资源动作。
     */
    public boolean isAllowed(CurrentUser user, String tenantCode, String obj, String action) {
        if (user == null) {
            return false;
        }
        if (user.isSuperAdmin()) {
            return true;
        }
        String subject = PlatformConstants.SUBJECT_USER_PREFIX + user.getUserId();
        if (!casbinEnforcerService.enforce(subject, tenantCode, obj, action)) {
            return false;
        }
        return apiPolicyService.evaluate(currentAbacContext(), obj, action);
    }

    public boolean isAllowedCurrent(String obj, String action) {
        if (!UserContext.isAuthenticated()) {
            return false;
        }
        return isAllowed(UserContext.require(), TenantContext.require().getCode(), obj, action);
    }

    public AbacContext currentAbacContext() {
        CurrentUser user = UserContext.get();
        AbacContext.AbacContextBuilder builder = AbacContext.builder()
                .userId(user == null ? null : user.getUserId())
                .username(user == null ? null : user.getUsername())
                .tenantId(user == null ? null : user.getTenantId())
                .tenantCode(user == null ? null : user.getTenantCode())
                .deptId(user == null ? null : user.getDeptId())
                .roles(user == null ? java.util.List.of() : user.getRoles())
                .now(LocalDateTime.now())
                .hour(LocalDateTime.now().getHour())
                .attributes(new HashMap<>());
        HttpServletRequest request = currentRequest();
        if (request != null) {
            builder.ip(WebUtils.clientIp(request));
        }
        return builder.build();
    }

    private HttpServletRequest currentRequest() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest();
        }
        return null;
    }
}
