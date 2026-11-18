package com.enterprise.platform.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 认证与安全配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "platform.auth")
public class SecurityProperties {

    /** internal：平台内置 JWT；keycloak：对接 Keycloak 的 JWKS。 */
    private String mode = "internal";

    /** 内置模式的签名密钥（HS256，至少 32 字节）。生产环境务必通过环境变量注入。 */
    private String secret = "platform-default-secret-change-me-in-production-2026";

    private String issuer = "enterprise-platform";

    private long accessTokenTtlSeconds = 7200;

    private long refreshTokenTtlSeconds = 604800;

    /** Keycloak 模式的 JWKS 地址，如 http://localhost:8080/realms/platform/protocol/openid-connect/certs。 */
    private String keycloakJwkSetUri;

    private String keycloakIssuer;

    /** 租户声明的 claim 名称（Keycloak 模式）。 */
    private String tenantClaim = "tenant_code";

    /** 免认证路径。 */
    private List<String> permitAll = new ArrayList<>(List.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/error",
            "/actuator/health",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    ));

    /** 是否开启 URL 级统一鉴权（应用层兜底，生产建议由网关承担）。 */
    private boolean urlCheckEnabled = false;
}
