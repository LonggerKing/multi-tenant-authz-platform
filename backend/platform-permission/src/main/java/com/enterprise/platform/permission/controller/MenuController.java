package com.enterprise.platform.permission.controller;

import com.enterprise.platform.common.api.ApiResult;
import com.enterprise.platform.permission.dto.PermissionDtos;
import com.enterprise.platform.permission.entity.SysMenu;
import com.enterprise.platform.permission.service.MenuService;
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
 * 菜单/资源管理。
 */
@RestController
@RequestMapping("/api/system/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/tree")
    @PreAuthorize("@perm.has('system:menu:list')")
    public ApiResult<List<PermissionDtos.MenuNode>> tree() {
        return ApiResult.ok(menuService.tree());
    }

    @GetMapping
    @PreAuthorize("@perm.has('system:menu:list')")
    public ApiResult<List<SysMenu>> list() {
        return ApiResult.ok(menuService.listAll());
    }

    @PostMapping
    @PreAuthorize("@perm.has('system:menu:create')")
    public ApiResult<SysMenu> create(@RequestBody @Valid PermissionDtos.MenuSaveRequest request) {
        return ApiResult.ok(menuService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('system:menu:update')")
    public ApiResult<SysMenu> update(@PathVariable Long id,
                                     @RequestBody @Valid PermissionDtos.MenuSaveRequest request) {
        return ApiResult.ok(menuService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('system:menu:delete')")
    public ApiResult<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return ApiResult.ok();
    }
}
