package com.enterprise.platform.permission.scopedata;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.platform.common.enums.DataScopeType;
import com.enterprise.platform.common.exception.BizException;
import com.enterprise.platform.common.exception.ErrorCode;
import com.enterprise.platform.common.tenant.TenantContext;
import com.enterprise.platform.common.security.UserContext;
import com.enterprise.platform.permission.cache.PermissionCacheService;
import com.enterprise.platform.permission.entity.SysDept;
import com.enterprise.platform.permission.entity.SysRole;
import com.enterprise.platform.permission.entity.SysUser;
import com.enterprise.platform.permission.entity.SysUserRole;
import com.enterprise.platform.permission.mapper.SysDeptMapper;
import com.enterprise.platform.permission.mapper.SysRoleMapper;
import com.enterprise.platform.permission.mapper.SysUserMapper;
import com.enterprise.platform.permission.mapper.SysUserRoleMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 行级数据权限求解：把用户的多个角色合并成一条可见范围。
 * <p>合并规则：任一角色为 ALL 则全量可见；否则部门集合取并集，"仅本人" 作为附加条件叠加。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataScopeService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysDeptMapper deptMapper;
    private final PermissionCacheService cacheService;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScopeFilter implements Serializable {
        private boolean all;
        private boolean selfOnly;
        private List<Long> deptIds = new ArrayList<>();
    }

    public ScopeFilter resolve(Long userId) {
        String cacheKey = "perm:scope:" + TenantContext.currentTenantCode() + ":" + userId;
        ScopeFilter filter = cacheService.get(cacheKey, new TypeReference<ScopeFilter>() {
        }, () -> doResolve(userId));
        return filter == null ? new ScopeFilter(false, true, new ArrayList<>()) : filter;
    }

    /**
     * 生成可直接拼接到 where 的 SQL 片段；返回 null 表示不受限。
     */
    public String buildCondition(String tableAlias, Long userId) {
        ScopeFilter filter = resolve(userId);
        if (filter.isAll()) {
            return null;
        }
        String prefix = StringUtils.hasText(tableAlias) ? tableAlias + "." : "";
        List<String> parts = new ArrayList<>(2);
        if (filter.getDeptIds() != null && !filter.getDeptIds().isEmpty()) {
            String ids = filter.getDeptIds().stream().map(String::valueOf).collect(Collectors.joining(","));
            parts.add(prefix + "dept_id in (" + ids + ")");
        }
        if (filter.isSelfOnly()) {
            parts.add(prefix + "create_by = " + userId);
        }
        if (parts.isEmpty()) {
            return "1 = 0";
        }
        return "(" + String.join(" or ", parts) + ")";
    }

    private ScopeFilter doResolve(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.of(ErrorCode.USER_NOT_FOUND);
        }
        List<Long> roleIds = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return new ScopeFilter(false, true, new ArrayList<>());
        }
        List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
        Set<Long> deptIds = new LinkedHashSet<>();
        boolean all = false;
        boolean selfOnly = false;
        for (SysRole role : roles) {
            if (role.getStatus() != null && role.getStatus() != 1) {
                continue;
            }
            DataScopeType scope = parseScope(role.getDataScope());
            switch (scope) {
                case ALL -> all = true;
                case SELF -> selfOnly = true;
                case DEPT -> {
                    if (user.getDeptId() != null) {
                        deptIds.add(user.getDeptId());
                    }
                }
                case DEPT_AND_CHILD -> deptIds.addAll(subtreeDeptIds(user.getDeptId()));
                case CUSTOM -> deptIds.addAll(parseIds(role.getCustomDeptIds()));
                default -> selfOnly = true;
            }
        }
        if (all) {
            return new ScopeFilter(true, false, new ArrayList<>());
        }
        if (deptIds.isEmpty()) {
            selfOnly = true;
        }
        return new ScopeFilter(false, selfOnly, new ArrayList<>(deptIds));
    }

    private List<Long> subtreeDeptIds(Long deptId) {
        if (deptId == null) {
            return List.of();
        }
        SysDept dept = deptMapper.selectById(deptId);
        if (dept == null || !StringUtils.hasText(dept.getPath())) {
            return List.of(deptId);
        }
        return deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                        .likeRight(SysDept::getPath, dept.getPath()))
                .stream().map(SysDept::getId).toList();
    }

    private DataScopeType parseScope(String scope) {
        if (!StringUtils.hasText(scope)) {
            return DataScopeType.SELF;
        }
        try {
            return DataScopeType.valueOf(scope);
        } catch (IllegalArgumentException e) {
            log.warn("[数据权限] 未知的范围类型: {}", scope);
            return DataScopeType.SELF;
        }
    }

    private List<Long> parseIds(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String part : raw.split(",")) {
            String value = part.trim();
            if (value.isEmpty()) {
                continue;
            }
            try {
                ids.add(Long.parseLong(value));
            } catch (NumberFormatException ignore) {
                // 忽略非法配置
            }
        }
        return ids;
    }

    public void evictCurrentTenant() {
        cacheService.evictByPrefix("perm:scope:" + TenantContext.currentTenantCode());
    }

    public void evictCurrentUser() {
        if (UserContext.isAuthenticated()) {
            cacheService.evictByPrefix("perm:scope:" + TenantContext.currentTenantCode()
                    + ":" + UserContext.currentUserId());
        }
    }
}
