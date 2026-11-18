package com.enterprise.platform.auth.jwt;

import com.enterprise.platform.auth.config.SecurityProperties;
import com.enterprise.platform.common.tenant.TenantContext;
import com.enterprise.platform.common.tenant.TenantInfo;
import com.enterprise.platform.common.security.CurrentUser;
import com.enterprise.platform.common.security.UserContext;
import com.enterprise.platform.common.util.JsonUtils;
import com.enterprise.platform.common.util.WebUtils;
import com.enterprise.platform.tenant.service.TenantService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 认证过滤器：解析令牌 → 建立安全上下文 / 租户上下文 / 当前用户上下文。
 * <p>令牌中的租户声明覆盖请求头，防止客户端伪造 X-Tenant-Id 跨租户访问。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenVerifier tokenVerifier;
    private final TenantService tenantService;
    private final SecurityProperties securityProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = WebUtils.bearerToken(request);
        if (token != null) {
            try {
                CurrentUser user = tokenVerifier.verify(token);
                if (user == null || user.getTenantCode() == null) {
                    writeUnauthorized(response, "令牌缺少租户信息");
                    return;
                }
                TenantInfo tenant = tenantService.resolve(user.getTenantCode());
                TenantContext.set(tenant);
                UserContext.set(user);
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(user, null, authorities(user)));
            } catch (Exception e) {
                log.warn("[认证] 令牌校验失败: {}", e.toString());
                writeUnauthorized(response, e.getMessage());
                return;
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private List<SimpleGrantedAuthority> authorities(CurrentUser user) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (user.getRoles() != null) {
            user.getRoles().forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        }
        return authorities;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(JsonUtils.toJson(
                Map.of("code", 40100, "message", message == null ? "未认证或凭证无效" : message)));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return securityProperties.getPermitAll().stream()
                .anyMatch(pattern -> matches(request.getRequestURI(), pattern));
    }

    private boolean matches(String path, String pattern) {
        if (pattern.endsWith("/**")) {
            return path.startsWith(pattern.substring(0, pattern.length() - 3));
        }
        return path.equals(pattern);
    }
}
