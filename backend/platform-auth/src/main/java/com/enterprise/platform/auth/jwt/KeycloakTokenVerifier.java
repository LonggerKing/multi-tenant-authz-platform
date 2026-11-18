package com.enterprise.platform.auth.jwt;

import com.enterprise.platform.auth.config.SecurityProperties;
import com.enterprise.platform.common.security.CurrentUser;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Keycloak 令牌校验器：通过 JWKS 远程公钥验签，并映射平台所需声明。
 * <p>约定 Keycloak 侧使用自定义 claim {@code tenant_code} 与 {@code platform_user_id}，
 * 具体映射规则见 docs/architecture.md。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "platform.auth.mode", havingValue = "keycloak")
public class KeycloakTokenVerifier implements TokenVerifier {

    private final SecurityProperties properties;
    private final ConfigurableJWTProcessor<SecurityContext> processor;

    public KeycloakTokenVerifier(SecurityProperties properties) {
        this.properties = properties;
        if (properties.getKeycloakJwkSetUri() == null || properties.getKeycloakJwkSetUri().isBlank()) {
            throw new IllegalStateException("keycloak 模式必须配置 platform.auth.keycloak-jwk-set-uri");
        }
        try {
            JWKSource<SecurityContext> keySource = new RemoteJWKSet<>(new URL(properties.getKeycloakJwkSetUri()));
            DefaultJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
            jwtProcessor.setJWSKeySelector(
                    new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource));
            jwtProcessor.setJWTClaimsSetVerifier(
                    new DefaultJWTClaimsVerifier<>(new JWTClaimsSet.Builder().build(),
                            new HashSet<>(List.of("sub", "exp"))));
            this.processor = jwtProcessor;
        } catch (MalformedURLException e) {
            throw new IllegalStateException("非法的 JWKS 地址: " + properties.getKeycloakJwkSetUri(), e);
        }
    }

    @Override
    public CurrentUser verify(String token) {
        try {
            JWTClaimsSet claims = processor.process(token, null);
            String tenantCode = claims.getStringClaim(properties.getTenantClaim());
            List<String> roles = new ArrayList<>();
            Object realmAccess = claims.getClaim("realm_access");
            if (realmAccess instanceof Map<?, ?> map && map.get("roles") instanceof List<?> list) {
                list.forEach(role -> roles.add(String.valueOf(role)));
            }
            String platformUserId = claims.getStringClaim("platform_user_id");
            return CurrentUser.builder()
                    .userId(platformUserId == null ? null : Long.valueOf(platformUserId))
                    .username(claims.getStringClaim("preferred_username"))
                    .nickname(claims.getStringClaim("name"))
                    .tenantCode(tenantCode)
                    .superAdmin(roles.contains("PLATFORM_ADMIN"))
                    .roles(roles)
                    .tokenId(claims.getJWTID())
                    .tokenType("access")
                    .build();
        } catch (Exception e) {
            log.warn("[Keycloak] 令牌校验失败: {}", e.getMessage());
            throw new JwtTokenService.TokenException("Keycloak 令牌无效: " + e.getMessage());
        }
    }
}
