package com.enterprise.platform.permission.controller;

import com.enterprise.platform.common.api.ApiResult;
import com.enterprise.platform.common.api.PageResult;
import com.enterprise.platform.common.security.UserContext;
import com.enterprise.platform.permission.dto.PermissionDtos;
import com.enterprise.platform.permission.service.UserService;
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
 * 用户管理。
 */
@RestController
@RequestMapping("/api/system/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("@perm.has('system:user:list')")
    public ApiResult<PageResult<PermissionDtos.UserVO>> page(@Valid PermissionDtos.UserQuery query) {
        return ApiResult.ok(userService.page(query));
    }

    @GetMapping("/me")
    public ApiResult<PermissionDtos.UserVO> me() {
        return ApiResult.ok(userService.detail(UserContext.currentUserId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('system:user:list')")
    public ApiResult<PermissionDtos.UserVO> detail(@PathVariable Long id) {
        return ApiResult.ok(userService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@perm.has('system:user:create')")
    public ApiResult<PermissionDtos.UserVO> create(@RequestBody @Valid PermissionDtos.UserCreateRequest request) {
        return ApiResult.ok(userService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('system:user:update')")
    public ApiResult<PermissionDtos.UserVO> update(@PathVariable Long id,
                                                   @RequestBody @Valid PermissionDtos.UserUpdateRequest request) {
        return ApiResult.ok(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('system:user:delete')")
    public ApiResult<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResult.ok();
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("@perm.has('system:user:reset')")
    public ApiResult<Void> resetPassword(@PathVariable Long id,
                                         @RequestBody @Valid PermissionDtos.PasswordRequest request) {
        userService.resetPassword(id, request.getPassword());
        return ApiResult.ok();
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("@perm.has('system:user:assign')")
    public ApiResult<Void> assignRoles(@PathVariable Long id,
                                       @RequestBody PermissionDtos.IdsRequest request) {
        List<Long> roleIds = request.getIds();
        userService.assignRoles(id, roleIds);
        return ApiResult.ok();
    }
}
