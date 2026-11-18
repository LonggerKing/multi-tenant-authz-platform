package com.enterprise.platform.permission.controller;

import com.enterprise.platform.common.api.ApiResult;
import com.enterprise.platform.permission.dto.PermissionDtos;
import com.enterprise.platform.permission.entity.SysDept;
import com.enterprise.platform.permission.service.DeptService;
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
 * 组织架构管理。
 */
@RestController
@RequestMapping("/api/system/depts")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @GetMapping("/tree")
    public ApiResult<List<PermissionDtos.DeptNode>> tree() {
        return ApiResult.ok(deptService.tree());
    }

    @GetMapping
    public ApiResult<List<SysDept>> list() {
        return ApiResult.ok(deptService.listAll());
    }

    @PostMapping
    @PreAuthorize("@perm.has('system:dept:create')")
    public ApiResult<SysDept> create(@RequestBody @Valid PermissionDtos.DeptSaveRequest request) {
        return ApiResult.ok(deptService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('system:dept:update')")
    public ApiResult<SysDept> update(@PathVariable Long id,
                                     @RequestBody @Valid PermissionDtos.DeptSaveRequest request) {
        return ApiResult.ok(deptService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('system:dept:delete')")
    public ApiResult<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return ApiResult.ok();
    }
}
