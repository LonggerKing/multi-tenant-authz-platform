package com.enterprise.platform.tenant.support;

import com.enterprise.platform.common.api.ApiResult;
import com.enterprise.platform.common.exception.BizException;
import com.enterprise.platform.common.tenant.TenantContext;
import com.enterprise.platform.common.tenant.TenantInfo;
import com.enterprise.platform.common.util.JsonUtils;
import com.enterprise.platform.common.util.WebUtils;
import com.enterprise.platform.tenant.config.TenantProperties;
import com.enterprise.platform.tenant.service.TenantService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 租户上下文解析过滤器。
 * <p>解析顺序：JWT 声明（由认证过滤器覆盖） &gt; 请求头 X-Tenant-Id &gt; 子域名。
 * 这里只做请求头/子域名解析，JWT 声明在认证过滤器中会进行覆盖，防止请求头伪造跨租户访问。</p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private final TenantProperties properties;
    private final TenantService tenantService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            if (properties.isEnabled() && !"OPTIONS".equalsIgnoreCase(request.getMethod())) {
                bindTenant(request);
            }
            filterChain.doFilter(request, response);
        } catch (BizException ex) {
            // 过滤器阶段异常不会进入 ControllerAdvice，这里统一输出
            log.warn("[租户解析] {} {} -> {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
            writeError(response, ex);
        } finally {
            TenantContext.clear();
        }
    }

    private void writeError(HttpServletResponse response, BizException ex) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(JsonUtils.toJson(ApiResult.fail(ex.getCode(), ex.getMessage())));
    }

    private void bindTenant(HttpServletRequest request) {
        String code = null;
        if (properties.isHeaderResolveEnabled()) {
            code = WebUtils.tenantHeader(request);
        }
        if (!StringUtils.hasText(code)) {
            code = resolveFromSubdomain(request);
        }
        if (!StringUtils.hasText(code)) {
            return;
        }
        TenantInfo info = tenantService.resolve(code);
        if (info != null) {
            TenantContext.set(info);
        }
    }

    private String resolveFromSubdomain(HttpServletRequest request) {
        String host = request.getServerName();
        if (!StringUtils.hasText(host) || host.indexOf('.') <= 0) {
            return null;
        }
        String sub = host.substring(0, host.indexOf('.'));
        return "www".equalsIgnoreCase(sub) || "localhost".equalsIgnoreCase(sub) ? null : sub;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/");
    }
}
