package com.enterprise.platform.permission.controller;

import com.enterprise.platform.common.api.ApiResult;
import com.enterprise.platform.common.api.PageResult;
import com.enterprise.platform.permission.dto.PermissionDtos;
import com.enterprise.platform.permission.service.RoleService;
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
 * 角色与授权管理。
 */
@RestController
@RequestMapping("/api/system/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("@perm.has('system:role:list')")
    public ApiResult<PageResult<PermissionDtos.RoleVO>> page(@Valid PermissionDtos.RoleQuery query) {
        return ApiResult.ok(roleService.page(query));
    }

    @GetMapping("/all")
    public ApiResult<List<PermissionDtos.RoleVO>> all() {
        return ApiResult.ok(roleService.listAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('system:role:list')")
    public ApiResult<PermissionDtos.RoleVO> detail(@PathVariable Long id) {
        return ApiResult.ok(roleService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@perm.has('system:role:create')")
    public ApiResult<PermissionDtos.RoleVO> create(@RequestBody @Valid PermissionDtos.RoleSaveRequest request) {
        return ApiResult.ok(roleService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('system:role:update')")
    public ApiResult<PermissionDtos.RoleVO> update(@PathVariable Long id,
                                                   @RequestBody @Valid PermissionDtos.RoleSaveRequest request) {
        return ApiResult.ok(roleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('system:role:delete')")
    public ApiResult<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ApiResult.ok();
    }

    @PutMapping("/{id}/menus")
    @PreAuthorize("@perm.has('system:role:assign')")
    public ApiResult<Void> assignMenus(@PathVariable Long id,
                                       @RequestBody PermissionDtos.IdsRequest request) {
        roleService.assignMenus(id, request.getIds());
        return ApiResult.ok();
    }

    @GetMapping("/{id}/menus")
    @PreAuthorize("@perm.has('system:role:list')")
    public ApiResult<List<Long>> menuIds(@PathVariable Long id) {
        return ApiResult.ok(roleService.menuIdsOf(id));
    }
}
