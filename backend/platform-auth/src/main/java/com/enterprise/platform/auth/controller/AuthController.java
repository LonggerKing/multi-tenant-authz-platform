package com.enterprise.platform.auth.controller;

import com.enterprise.platform.auth.dto.AuthDtos;
import com.enterprise.platform.auth.service.AuthService;
import com.enterprise.platform.common.api.ApiResult;
import com.enterprise.platform.permission.dto.PermissionDtos;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证入口。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResult<AuthDtos.LoginResponse> login(@RequestBody @Valid AuthDtos.LoginRequest request,
                                                   HttpServletRequest httpRequest) {
        return ApiResult.ok(authService.login(request, httpRequest));
    }

    @PostMapping("/refresh")
    public ApiResult<AuthDtos.LoginResponse> refresh(@RequestBody @Valid AuthDtos.RefreshRequest request,
                                                     HttpServletRequest httpRequest) {
        return ApiResult.ok(authService.refresh(request.getRefreshToken(), httpRequest));
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout() {
        authService.logout();
        return ApiResult.ok();
    }

    /**
     * 当前登录用户的角色、权限点、菜单树与数据范围。
     */
    @GetMapping("/userinfo")
    public ApiResult<PermissionDtos.UserPermissionView> userInfo() {
        return ApiResult.ok(authService.userInfo());
    }
}
