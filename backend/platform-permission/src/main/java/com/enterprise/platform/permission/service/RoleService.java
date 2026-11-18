package com.enterprise.platform.permission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.platform.common.api.PageResult;
import com.enterprise.platform.common.constant.PlatformConstants;
import com.enterprise.platform.common.exception.BizException;
import com.enterprise.platform.common.exception.ErrorCode;
import com.enterprise.platform.common.tenant.TenantContext;
import com.enterprise.platform.permission.cache.PermissionCacheService;
import com.enterprise.platform.permission.casbin.CasbinEnforcerService;
import com.enterprise.platform.permission.casbin.CasbinRuleRepository;
import com.enterprise.platform.permission.dto.PermissionDtos;
import com.enterprise.platform.permission.entity.SysMenu;
import com.enterprise.platform.permission.entity.SysRole;
import com.enterprise.platform.permission.entity.SysRoleMenu;
import com.enterprise.platform.permission.entity.SysUserRole;
import com.enterprise.platform.permission.mapper.SysMenuMapper;
import com.enterprise.platform.permission.mapper.SysRoleMapper;
import com.enterprise.platform.permission.mapper.SysRoleMenuMapper;
import com.enterprise.platform.permission.mapper.SysUserRoleMapper;
import com.enterprise.platform.permission.scopedata.DataScopeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色与授权。角色-菜单关系是 Casbin 策略的唯一事实来源，变更后即时重建策略。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final CasbinRuleRepository casbinRuleRepository;
    private final CasbinEnforcerService casbinEnforcerService;
    private final PermissionCacheService cacheService;
    private final DataScopeService dataScopeService;

    public PageResult<PermissionDtos.RoleVO> page(PermissionDtos.RoleQuery query) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(SysRole::getName, kw).or().like(SysRole::getCode, kw));
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysRole::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(SysRole::getBuiltin).orderByDesc(SysRole::getCreateTime);
        Page<SysRole> page = roleMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<PermissionDtos.RoleVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    public List<PermissionDtos.RoleVO> listAll() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getStatus, 1).orderByAsc(SysRole::getName))
                .stream().map(this::toVO).toList();
    }

    public PermissionDtos.RoleVO detail(Long id) {
        return toVO(getRole(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public PermissionDtos.RoleVO create(PermissionDtos.RoleSaveRequest request) {
        Long exists = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, request.getCode()));
        if (exists != null && exists > 0) {
            throw BizException.of(ErrorCode.CONFLICT, "角色编码已存在: " + request.getCode());
        }
        SysRole role = new SysRole();
        apply(role, request);
        role.setTenantId(TenantContext.currentTenantId());
        role.setBuiltin(false);
        roleMapper.insert(role);
        return toVO(role);
    }

    @Transactional(rollbackFor = Exception.class)
    public PermissionDtos.RoleVO update(Long id, PermissionDtos.RoleSaveRequest request) {
        SysRole role = getRole(id);
        if (Boolean.TRUE.equals(role.getBuiltin()) && !role.getCode().equals(request.getCode())) {
            throw BizException.of(ErrorCode.BAD_REQUEST, "内置角色编码不可修改");
        }
        String previousCode = role.getCode();
        apply(role, request);
        roleMapper.updateById(role);
        if (!previousCode.equals(role.getCode())) {
            // 编码变更：旧策略失效，按新编码重建，避免残留授权
            casbinRuleRepository.deleteByRole(role.getTenantId(), previousCode);
        }
        rebuildPolicies(role);
        evictTenant();
        return toVO(role);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysRole role = getRole(id);
        if (Boolean.TRUE.equals(role.getBuiltin())) {
            throw BizException.of(ErrorCode.BAD_REQUEST, "内置角色不允许删除");
        }
        roleMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
        casbinRuleRepository.deleteByRole(role.getTenantId(), role.getCode());
        casbinEnforcerService.reload();
        evictTenant();
        log.info("[角色] 已删除角色 {} 及其授权", role.getCode());
    }

    /**
     * 覆盖角色的菜单/资源授权，并同步 Casbin 策略。
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        SysRole role = getRole(roleId);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        if (menuIds != null) {
            for (Long menuId : menuIds) {
                SysRoleMenu relation = new SysRoleMenu();
                relation.setTenantId(TenantContext.currentTenantId());
                relation.setRoleId(roleId);
                relation.setMenuId(menuId);
                roleMenuMapper.insert(relation);
            }
        }
        rebuildPolicies(role);
        evictTenant();
    }

    public List<Long> menuIdsOf(Long roleId) {
        return roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getRoleId, roleId))
                .stream().map(SysRoleMenu::getMenuId).toList();
    }

    /**
     * 依据角色-菜单关系重建该角色的全部 Casbin 策略。
     */
    public void rebuildPolicies(SysRole role) {
        List<Long> menuIds = menuIdsOf(role.getId());
        List<List<String>> policies = new ArrayList<>();
        String tenantCode = TenantContext.currentTenantCode();
        if (!menuIds.isEmpty()) {
            List<SysMenu> menus = menuMapper.selectBatchIds(menuIds);
            for (SysMenu menu : menus) {
                if (menu.getStatus() != null && menu.getStatus() != 1) {
                    continue;
                }
                if ("API".equalsIgnoreCase(menu.getType()) && StringUtils.hasText(menu.getPath())) {
                    String method = StringUtils.hasText(menu.getHttpMethod()) ? menu.getHttpMethod() : PlatformConstants.WILDCARD;
                    policies.add(List.of(role.getCode(), tenantCode, menu.getPath(), method, "allow"));
                } else if (StringUtils.hasText(menu.getCode())) {
                    policies.add(List.of(role.getCode(), tenantCode, menu.getCode(), PlatformConstants.WILDCARD, "allow"));
                }
            }
        }
        casbinRuleRepository.replaceRolePolicies(role.getTenantId(), role.getCode(), policies);
        casbinEnforcerService.reload();
        log.info("[角色] {} 策略重建完成，共 {} 条", role.getCode(), policies.size());
    }

    public List<String> roleCodesOfUser(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectBatchIds(roleIds).stream()
                .filter(r -> r.getStatus() == null || r.getStatus() == 1)
                .map(SysRole::getCode).toList();
    }

    public List<SysRole> rolesOfUser(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
        return roleIds.isEmpty() ? List.of() : roleMapper.selectBatchIds(roleIds);
    }

    public SysRole getRole(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw BizException.of(ErrorCode.ROLE_NOT_FOUND);
        }
        return role;
    }

    private void apply(SysRole role, PermissionDtos.RoleSaveRequest request) {
        role.setCode(request.getCode());
        role.setName(request.getName());
        role.setDataScope(StringUtils.hasText(request.getDataScope()) ? request.getDataScope() : "SELF");
        role.setCustomDeptIds(request.getCustomDeptIds() == null || request.getCustomDeptIds().isEmpty()
                ? null
                : request.getCustomDeptIds().stream().map(String::valueOf)
                .reduce((a, b) -> a + "," + b).orElse(null));
        role.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        role.setRemark(request.getRemark());
    }

    private PermissionDtos.RoleVO toVO(SysRole role) {
        PermissionDtos.RoleVO vo = new PermissionDtos.RoleVO();
        vo.setId(role.getId());
        vo.setCode(role.getCode());
        vo.setName(role.getName());
        vo.setDataScope(role.getDataScope());
        vo.setStatus(role.getStatus());
        vo.setBuiltin(role.getBuiltin());
        vo.setRemark(role.getRemark());
        if (StringUtils.hasText(role.getCustomDeptIds())) {
            List<Long> ids = new ArrayList<>();
            for (String part : role.getCustomDeptIds().split(",")) {
                if (!part.isBlank()) {
                    ids.add(Long.parseLong(part.trim()));
                }
            }
            vo.setCustomDeptIds(ids);
        }
        vo.setMenuIds(menuIdsOf(role.getId()));
        return vo;
    }

    private void evictTenant() {
        cacheService.evictByPrefix("perm:decision:" + TenantContext.currentTenantCode());
        cacheService.evictByPrefix("perm:menu:" + TenantContext.currentTenantCode());
        cacheService.evictByPrefix("perm:codes:" + TenantContext.currentTenantCode());
        dataScopeService.evictCurrentTenant();
    }
}
