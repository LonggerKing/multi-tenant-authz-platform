package com.enterprise.platform.auth.jwt;

import com.enterprise.platform.common.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 内置 JWT 校验器（默认）。
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "platform.auth.mode", havingValue = "internal", matchIfMissing = true)
public class InternalTokenVerifier implements TokenVerifier {

    private final JwtTokenService jwtTokenService;

    @Override
    public CurrentUser verify(String token) {
        return jwtTokenService.parse(token, "access");
    }
}
