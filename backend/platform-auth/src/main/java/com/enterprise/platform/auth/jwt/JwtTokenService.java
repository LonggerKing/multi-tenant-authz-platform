package com.enterprise.platform.auth.jwt;

import com.enterprise.platform.auth.config.SecurityProperties;
import com.enterprise.platform.common.security.CurrentUser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 内置 JWT 服务（HS256）。
 * <p>登出采用 jti 黑名单，本地缓存实现；集群部署时把黑名单切到 Redis 即可（接口保持不变）。</p>
 */
@Slf4j
@Component
public class JwtTokenService {

    private static final String CLAIM_TENANT_ID = "tid";
    private static final String CLAIM_TENANT_CODE = "tcode";
    private static final String CLAIM_USERNAME = "uname";
    private static final String CLAIM_NICKNAME = "nick";
    private static final String CLAIM_DEPT = "dept";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_SUPER_ADMIN = "sa";
    private static final String CLAIM_TYPE = "typ";

    private final SecurityProperties properties;
    private final byte[] secret;
    private final Cache<String, Boolean> blacklist;

    public JwtTokenService(SecurityProperties properties) {
        this.properties = properties;
        this.secret = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("platform.auth.secret 至少需要 32 字节");
        }
        this.blacklist = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(properties.getAccessTokenTtlSeconds() + 60))
                .maximumSize(100_000)
                .build();
    }

    public String createAccessToken(CurrentUser user) {
        return createToken(user, "access", properties.getAccessTokenTtlSeconds());
    }

    public String createRefreshToken(CurrentUser user) {
        return createToken(user, "refresh", properties.getRefreshTokenTtlSeconds());
    }

    public CurrentUser parse(String token, String expectedType) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(new MACVerifier(secret))) {
                throw new TokenException("签名校验失败");
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date expiration = claims.getExpirationTime();
            if (expiration == null || expiration.toInstant().isBefore(Instant.now())) {
                throw new TokenException("令牌已过期");
            }
            String type = claims.getStringClaim(CLAIM_TYPE);
            if (expectedType != null && !expectedType.equals(type)) {
                throw new TokenException("令牌类型不匹配");
            }
            String jti = claims.getJWTID();
            if (jti != null && blacklist.getIfPresent(jti) != null) {
                throw new TokenException("令牌已失效");
            }
            List<String> roles = new ArrayList<>();
            Object rawRoles = claims.getClaim(CLAIM_ROLES);
            if (rawRoles instanceof List<?> list) {
                list.forEach(item -> roles.add(String.valueOf(item)));
            }
            Boolean superAdmin = claims.getBooleanClaim(CLAIM_SUPER_ADMIN);
            return CurrentUser.builder()
                    .userId(Long.valueOf(claims.getSubject()))
                    .username(claims.getStringClaim(CLAIM_USERNAME))
                    .nickname(claims.getStringClaim(CLAIM_NICKNAME))
                    .tenantId(numericClaim(claims, CLAIM_TENANT_ID))
                    .tenantCode(claims.getStringClaim(CLAIM_TENANT_CODE))
                    .deptId(numericClaim(claims, CLAIM_DEPT))
                    .superAdmin(Boolean.TRUE.equals(superAdmin))
                    .roles(roles)
                    .tokenId(jti)
                    .tokenType(type)
                    .build();
        } catch (TokenException e) {
            throw e;
        } catch (Exception e) {
            throw new TokenException("令牌解析失败: " + e.getMessage());
        }
    }

    public void revoke(String tokenId) {
        if (tokenId != null) {
            blacklist.put(tokenId, Boolean.TRUE);
        }
    }

    /**
     * 数字声明容错读取：JSON 反序列化后可能是 Integer / Long / String。
     */
    private Long numericClaim(JWTClaimsSet claims, String name) {
        Object value = claims.getClaim(name);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public long accessTokenTtlSeconds() {
        return properties.getAccessTokenTtlSeconds();
    }

    private String createToken(CurrentUser user, String type, long ttlSeconds) {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(String.valueOf(user.getUserId()))
                .issuer(properties.getIssuer())
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(ttlSeconds)))
                .claim(CLAIM_TENANT_ID, user.getTenantId())
                .claim(CLAIM_TENANT_CODE, user.getTenantCode())
                .claim(CLAIM_USERNAME, user.getUsername())
                .claim(CLAIM_NICKNAME, user.getNickname())
                .claim(CLAIM_DEPT, user.getDeptId())
                .claim(CLAIM_ROLES, user.getRoles())
                .claim(CLAIM_SUPER_ADMIN, user.isSuperAdmin())
                .claim(CLAIM_TYPE, type)
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            jwt.sign(new MACSigner(secret));
        } catch (JOSEException e) {
            throw new IllegalStateException("令牌签发失败", e);
        }
        return jwt.serialize();
    }

    /**
     * 令牌校验异常。
     */
    public static class TokenException extends RuntimeException {
        public TokenException(String message) {
            super(message);
        }
    }
}
