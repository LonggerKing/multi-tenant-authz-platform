package com.enterprise.platform.permission.controller;

import com.enterprise.platform.common.api.ApiResult;
import com.enterprise.platform.common.tenant.TenantContext;
import com.enterprise.platform.permission.abac.ApiPolicyService;
import com.enterprise.platform.permission.entity.SysApiPolicy;
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
 * ABAC 条件策略管理（属性级动态授权）。
 */
@RestController
@RequestMapping("/api/system/abac-policies")
@RequiredArgsConstructor
public class ApiPolicyController {

    private final ApiPolicyService apiPolicyService;

    @GetMapping
    @PreAuthorize("@perm.has('system:abac:list')")
    public ApiResult<List<SysApiPolicy>> list() {
        return ApiResult.ok(apiPolicyService.list());
    }

    @PostMapping
    @PreAuthorize("@perm.has('system:abac:create')")
    public ApiResult<Void> create(@RequestBody SysApiPolicy policy) {
        policy.setId(null);
        policy.setTenantId(TenantContext.currentTenantId());
        apiPolicyService.save(policy);
        return ApiResult.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('system:abac:update')")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody SysApiPolicy policy) {
        policy.setId(id);
        apiPolicyService.save(policy);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('system:abac:delete')")
    public ApiResult<Void> delete(@PathVariable Long id) {
        apiPolicyService.delete(id);
        return ApiResult.ok();
    }
}
