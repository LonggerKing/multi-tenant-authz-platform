package com.enterprise.platform.auth.controller;

import com.enterprise.platform.auth.entity.SysLoginLog;
import com.enterprise.platform.auth.service.LoginLogService;
import com.enterprise.platform.common.api.ApiResult;
import com.enterprise.platform.common.api.PageQuery;
import com.enterprise.platform.common.api.PageResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录审计查询。
 */
@RestController
@RequestMapping("/api/system/login-logs")
@RequiredArgsConstructor
public class LoginLogController {

    private final LoginLogService loginLogService;

    @GetMapping
    @PreAuthorize("@perm.has('system:log:list')")
    public ApiResult<PageResult<SysLoginLog>> page(@Valid PageQuery query) {
        return ApiResult.ok(loginLogService.page(query));
    }
}
