package com.enterprise.platform.tenant.controller;

import com.enterprise.platform.common.annotation.RequireSuperAdmin;
import com.enterprise.platform.common.api.ApiResult;
import com.enterprise.platform.common.api.PageResult;
import com.enterprise.platform.common.tenant.TenantContext;
import com.enterprise.platform.common.tenant.TenantInfo;
import com.enterprise.platform.tenant.dto.TenantDtos;
import com.enterprise.platform.tenant.entity.SysTenant;
import com.enterprise.platform.tenant.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 租户管理（平台侧）。
 */
@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @GetMapping
    @RequireSuperAdmin
    @PreAuthorize("@perm.superAdmin()")
    public ApiResult<PageResult<SysTenant>> page(@Valid TenantDtos.Query query) {
        return ApiResult.ok(tenantService.page(query));
    }

    @GetMapping("/all")
    @PreAuthorize("@perm.superAdmin()")
    public ApiResult<List<SysTenant>> all() {
        return ApiResult.ok(tenantService.listAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.superAdmin()")
    public ApiResult<SysTenant> detail(@PathVariable Long id) {
        return ApiResult.ok(tenantService.getById(id));
    }

    @PostMapping
    @PreAuthorize("@perm.superAdmin()")
    public ApiResult<SysTenant> create(@RequestBody @Valid TenantDtos.CreateRequest request) {
        return ApiResult.ok(tenantService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.superAdmin()")
    public ApiResult<SysTenant> update(@PathVariable Long id,
                                       @RequestBody @Valid TenantDtos.UpdateRequest request) {
        return ApiResult.ok(tenantService.update(id, request));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@perm.superAdmin()")
    public ApiResult<Void> changeStatus(@PathVariable Long id,
                                        @RequestBody TenantDtos.StatusRequest request) {
        tenantService.changeStatus(id, request.getStatus());
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.superAdmin()")
    public ApiResult<Void> delete(@PathVariable Long id) {
        tenantService.delete(id);
        return ApiResult.ok();
    }

    /**
     * 当前请求命中的租户信息（任意登录用户可读）。
     */
    @GetMapping("/current")
    public ApiResult<TenantInfo> current() {
        return ApiResult.ok(TenantContext.require());
    }
}
