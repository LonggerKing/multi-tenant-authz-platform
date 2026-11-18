package com.enterprise.platform.auth.jwt;

import com.enterprise.platform.common.security.CurrentUser;

/**
 * 令牌校验抽象：内置 JWT 与 Keycloak 两种实现按配置装配。
 */
public interface TokenVerifier {

    CurrentUser verify(String token);
}
