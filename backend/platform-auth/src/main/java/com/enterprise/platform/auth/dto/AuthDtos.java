package com.enterprise.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 认证相关 DTO。
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "租户编码不能为空")
        private String tenantCode;

        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class RefreshRequest {
        @NotBlank(message = "refreshToken 不能为空")
        private String refreshToken;
    }

    @Data
    public static class LoginResponse implements Serializable {
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";
        private long expiresIn;
        private Long userId;
        private String username;
        private String nickname;
        private String tenantCode;
        private String tenantName;
        private boolean superAdmin;
        private List<String> roles = new ArrayList<>();
    }
}
