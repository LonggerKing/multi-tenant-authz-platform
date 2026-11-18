package com.enterprise.platform.permission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.platform.common.tenant.TenantContext;
import com.enterprise.platform.permission.cache.PermissionCacheService;
import com.enterprise.platform.permission.dto.PermissionDtos;
import com.enterprise.platform.permission.entity.SysMenu;
import com.enterprise.platform.permission.entity.SysRoleMenu;
import com.enterprise.platform.permission.entity.SysUserRole;
import com.enterprise.platform.permission.mapper.SysMenuMapper;
import com.enterprise.platform.permission.mapper.SysRoleMenuMapper;
import com.enterprise.platform.permission.mapper.SysUserRoleMapper;
import com.enterprise.platform.permission.scopedata.DataScopeService;
import com.enterprise.platform.permission.support.TreeBuilder;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 登录态权限视图：角色、按钮权限点、菜单树。全部走多级缓存，避免登录后每次刷新都打库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;
    private final PermissionCacheService cacheService;
    private final DataScopeService dataScopeService;
    private final UserService userService;

    public PermissionDtos.UserPermissionView load(Long userId) {
        String tenantCode = TenantContext.currentTenantCode();
        PermissionDtos.UserPermissionView view = new PermissionDtos.UserPermissionView();
        view.setUser(userService.detail(userId));
        view.setRoles(roleCodes(tenantCode, userId));
        view.setPermissions(permissionCodes(tenantCode, userId));
        view.setMenus(menuTree(tenantCode, userId));
        view.setDataScope(scopeLabel(userId));
        return view;
    }

    public List<String> roleCodes(String tenantCode, Long userId) {
        List<String> cached = cacheService.get("perm:roles:" + tenantCode + ":" + userId,
                new TypeReference<List<String>>() {
                },
                () -> userService.roleCodes(userId));
        return cached == null ? List.of() : cached;
    }

    public List<String> permissionCodes(String tenantCode, Long userId) {
        List<String> cached = cacheService.get("perm:codes:" + tenantCode + ":" + userId,
                new TypeReference<List<String>>() {
                },
                () -> {
                    Set<String> codes = new LinkedHashSet<>();
                    for (SysMenu menu : authorizedMenus(userId)) {
                        if (StringUtils.hasText(menu.getCode()) && !"API".equalsIgnoreCase(menu.getType())) {
                            codes.add(menu.getCode());
                        }
                    }
                    return new ArrayList<>(codes);
                });
        return cached == null ? List.of() : cached;
    }

    public List<PermissionDtos.MenuNode> menuTree(String tenantCode, Long userId) {
        List<PermissionDtos.MenuNode> cached = cacheService.get("perm:menu:" + tenantCode + ":" + userId,
                new TypeReference<List<PermissionDtos.MenuNode>>() {
                },
                () -> {
                    List<SysMenu> menuList = authorizedMenus(userId).stream()
                            .filter(m -> "DIR".equalsIgnoreCase(m.getType()) || "MENU".equalsIgnoreCase(m.getType()))
                            .filter(m -> m.getVisible() == null || m.getVisible() == 1)
                            .sorted(Comparator.comparing(m -> m.getSort() == null ? 0 : m.getSort()))
                            .toList();
                    return TreeBuilder.buildMenuTree(menuList);
                });
        return cached == null ? List.of() : cached;
    }

    /**
     * 用户可见的菜单/资源全集（含按钮与接口资源）。
     */
    public List<SysMenu> authorizedMenus(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<Long> menuIds = roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .in(SysRoleMenu::getRoleId, roleIds))
                .stream().map(SysRoleMenu::getMenuId).distinct().toList();
        if (menuIds.isEmpty()) {
            return List.of();
        }
        return menuMapper.selectBatchIds(menuIds).stream()
                .filter(m -> m.getStatus() == null || m.getStatus() == 1)
                .toList();
    }

    public void evictUser(Long userId) {
        String tenantCode = TenantContext.currentTenantCode();
        cacheService.evictByPrefix("perm:roles:" + tenantCode + ":" + userId);
        cacheService.evictByPrefix("perm:codes:" + tenantCode + ":" + userId);
        cacheService.evictByPrefix("perm:menu:" + tenantCode + ":" + userId);
    }

    private String scopeLabel(Long userId) {
        DataScopeService.ScopeFilter filter = dataScopeService.resolve(userId);
        if (filter.isAll()) {
            return "ALL";
        }
        if (filter.getDeptIds() == null || filter.getDeptIds().isEmpty()) {
            return "SELF";
        }
        return filter.isSelfOnly() ? "DEPT_AND_SELF" : "DEPT";
    }
}
