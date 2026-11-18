package com.enterprise.platform.auth.service;

import com.enterprise.platform.auth.config.SecurityProperties;
import com.enterprise.platform.auth.dto.AuthDtos;
import com.enterprise.platform.auth.jwt.JwtTokenService;
import com.enterprise.platform.common.exception.BizException;
import com.enterprise.platform.common.exception.ErrorCode;
import com.enterprise.platform.common.security.CurrentUser;
import com.enterprise.platform.common.security.UserContext;
import com.enterprise.platform.common.tenant.TenantContext;
import com.enterprise.platform.common.util.WebUtils;
import com.enterprise.platform.permission.dto.PermissionDtos;
import com.enterprise.platform.permission.entity.SysUser;
import com.enterprise.platform.permission.service.PermissionService;
import com.enterprise.platform.permission.service.RoleService;
import com.enterprise.platform.permission.service.UserService;
import com.enterprise.platform.tenant.entity.SysTenant;
import com.enterprise.platform.tenant.service.TenantCache;
import com.enterprise.platform.tenant.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 登录 / 刷新 / 登出 / 权限视图。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final TenantService tenantService;
    private final UserService userService;
    private final RoleService roleService;
    private final PermissionService permissionService;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;
    private final LoginLogService loginLogService;
    private final SecurityProperties securityProperties;

    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request, HttpServletRequest httpRequest) {
        String ip = WebUtils.clientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        SysTenant tenant = tenantService.requireActiveByCode(request.getTenantCode());
        TenantContext.set(TenantCache.toInfo(tenant));

        SysUser user = userService.findByUsername(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginLogService.record(request.getUsername(), ip, userAgent, false, "用户名或密码错误");
            throw BizException.of(ErrorCode.LOGIN_FAILED);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            loginLogService.record(request.getUsername(), ip, userAgent, false, "账号已停用");
            throw BizException.of(ErrorCode.ACCOUNT_DISABLED);
        }

        List<String> roleCodes = roleService.roleCodesOfUser(user.getId());
        CurrentUser currentUser = toCurrentUser(user, tenant, roleCodes);
        String accessToken = jwtTokenService.createAccessToken(currentUser);
        String refreshToken = jwtTokenService.createRefreshToken(currentUser);

        userService.markLogin(user.getId());
        loginLogService.record(user.getUsername(), ip, userAgent, true, "登录成功");
        log.info("[登录] 租户={}, 用户={}, IP={}", tenant.getCode(), user.getUsername(), ip);

        AuthDtos.LoginResponse response = new AuthDtos.LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(jwtTokenService.accessTokenTtlSeconds());
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setTenantCode(tenant.getCode());
        response.setTenantName(tenant.getName());
        response.setSuperAdmin(Boolean.TRUE.equals(user.getSuperAdmin()));
        response.setRoles(roleCodes);
        return response;
    }

    public AuthDtos.LoginResponse refresh(String refreshToken, HttpServletRequest httpRequest) {
        if (!"internal".equalsIgnoreCase(securityProperties.getMode())) {
            throw BizException.of(ErrorCode.BAD_REQUEST, "Keycloak 模式请使用 Keycloak 的刷新端点");
        }
        CurrentUser parsed = jwtTokenService.parse(refreshToken, "refresh");
        SysTenant tenant = tenantService.requireActiveByCode(parsed.getTenantCode());
        TenantContext.set(TenantCache.toInfo(tenant));

        SysUser user = userService.getUser(parsed.getUserId());
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw BizException.of(ErrorCode.ACCOUNT_DISABLED);
        }
        List<String> roleCodes = roleService.roleCodesOfUser(user.getId());
        CurrentUser currentUser = toCurrentUser(user, tenant, roleCodes);

        AuthDtos.LoginResponse response = new AuthDtos.LoginResponse();
        response.setAccessToken(jwtTokenService.createAccessToken(currentUser));
        response.setRefreshToken(jwtTokenService.createRefreshToken(currentUser));
        response.setExpiresIn(jwtTokenService.accessTokenTtlSeconds());
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setTenantCode(tenant.getCode());
        response.setTenantName(tenant.getName());
        response.setSuperAdmin(Boolean.TRUE.equals(user.getSuperAdmin()));
        response.setRoles(roleCodes);
        loginLogService.record(user.getUsername(), WebUtils.clientIp(httpRequest),
                httpRequest.getHeader("User-Agent"), true, "刷新令牌");
        return response;
    }

    public void logout() {
        if (UserContext.isAuthenticated()) {
            jwtTokenService.revoke(UserContext.require().getTokenId());
        }
    }

    public PermissionDtos.UserPermissionView userInfo() {
        return permissionService.load(UserContext.currentUserId());
    }

    private CurrentUser toCurrentUser(SysUser user, SysTenant tenant, List<String> roleCodes) {
        return CurrentUser.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .tenantId(tenant.getId())
                .tenantCode(tenant.getCode())
                .deptId(user.getDeptId())
                .superAdmin(Boolean.TRUE.equals(user.getSuperAdmin()))
                .roles(roleCodes)
                .build();
    }
}
