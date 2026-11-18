package com.enterprise.platform.permission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.platform.common.api.PageResult;
import com.enterprise.platform.common.constant.PlatformConstants;
import com.enterprise.platform.common.exception.BizException;
import com.enterprise.platform.common.exception.ErrorCode;
import com.enterprise.platform.common.security.UserContext;
import com.enterprise.platform.common.tenant.TenantContext;
import com.enterprise.platform.permission.cache.PermissionCacheService;
import com.enterprise.platform.permission.casbin.CasbinEnforcerService;
import com.enterprise.platform.permission.casbin.CasbinRuleRepository;
import com.enterprise.platform.permission.dto.PermissionDtos;
import com.enterprise.platform.permission.entity.SysDept;
import com.enterprise.platform.permission.entity.SysRole;
import com.enterprise.platform.permission.entity.SysUser;
import com.enterprise.platform.permission.entity.SysUserRole;
import com.enterprise.platform.permission.mapper.SysDeptMapper;
import com.enterprise.platform.permission.mapper.SysRoleMapper;
import com.enterprise.platform.permission.mapper.SysUserMapper;
import com.enterprise.platform.permission.mapper.SysUserRoleMapper;
import com.enterprise.platform.permission.scopedata.DataScopeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户管理。用户-角色关系同时写入关系表与 Casbin g 策略。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysDeptMapper deptMapper;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final CasbinRuleRepository casbinRuleRepository;
    private final CasbinEnforcerService casbinEnforcerService;
    private final PermissionCacheService cacheService;
    private final DataScopeService dataScopeService;

    public PageResult<PermissionDtos.UserVO> page(PermissionDtos.UserQuery query) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(SysUser::getUsername, kw)
                    .or().like(SysUser::getNickname, kw)
                    .or().like(SysUser::getPhone, kw));
        }
        if (query.getDeptId() != null) {
            wrapper.eq(SysUser::getDeptId, query.getDeptId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(SysUser::getCreateTime);

        Page<SysUser> page = userMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<PermissionDtos.UserVO> records = enrich(page.getRecords());
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    public PermissionDtos.UserVO detail(Long id) {
        SysUser user = getUser(id);
        return enrich(List.of(user)).get(0);
    }

    @Transactional(rollbackFor = Exception.class)
    public PermissionDtos.UserVO create(PermissionDtos.UserCreateRequest request) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (exists != null && exists > 0) {
            throw BizException.of(ErrorCode.CONFLICT, "用户名已存在: " + request.getUsername());
        }
        SysUser user = new SysUser();
        user.setTenantId(TenantContext.currentTenantId());
        user.setUsername(request.getUsername());
        user.setNickname(StringUtils.hasText(request.getNickname())
                ? request.getNickname() : request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setDeptId(request.getDeptId());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        user.setSuperAdmin(false);
        userMapper.insert(user);

        assignRoles(user.getId(), request.getRoleIds());
        return enrich(List.of(user)).get(0);
    }

    @Transactional(rollbackFor = Exception.class)
    public PermissionDtos.UserVO update(Long id, PermissionDtos.UserUpdateRequest request) {
        SysUser user = getUser(id);
        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname());
        }
        if (request.getDeptId() != null) {
            user.setDeptId(request.getDeptId());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        userMapper.updateById(user);

        if (request.getRoleIds() != null) {
            assignRoles(id, request.getRoleIds());
        }
        evictUser(id);
        dataScopeService.evictCurrentTenant();
        return enrich(List.of(user)).get(0);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysUser user = getUser(id);
        if (UserContext.isAuthenticated() && id.equals(UserContext.currentUserId())) {
            throw BizException.of(ErrorCode.BAD_REQUEST, "不能删除当前登录账号");
        }
        if (Boolean.TRUE.equals(user.getSuperAdmin())) {
            throw BizException.of(ErrorCode.BAD_REQUEST, "平台超管账号不允许删除");
        }
        userMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        casbinRuleRepository.replaceUserRoles(user.getTenantId(), TenantContext.currentTenantCode(),
                PlatformConstants.SUBJECT_USER_PREFIX + id, List.of());
        casbinEnforcerService.reload();
        evictUser(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String rawPassword) {
        SysUser user = getUser(id);
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(rawPassword));
        userMapper.updateById(update);
        log.info("[用户] {} 密码已重置", user.getUsername());
    }

    /**
     * 覆盖用户角色：关系表 + Casbin g 策略 + 缓存失效。
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        getUser(userId);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        List<String> roleCodes = new ArrayList<>();
        if (roleIds != null && !roleIds.isEmpty()) {
            List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
            for (SysRole role : roles) {
                SysUserRole relation = new SysUserRole();
                relation.setTenantId(TenantContext.currentTenantId());
                relation.setUserId(userId);
                relation.setRoleId(role.getId());
                userRoleMapper.insert(relation);
                roleCodes.add(role.getCode());
            }
        }
        casbinRuleRepository.replaceUserRoles(TenantContext.currentTenantId(),
                TenantContext.currentTenantCode(),
                PlatformConstants.SUBJECT_USER_PREFIX + userId, roleCodes);
        casbinEnforcerService.reload();
        evictUser(userId);
        dataScopeService.evictCurrentTenant();
    }

    public SysUser findByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username).last("limit 1"));
    }

    public void markLogin(Long userId) {
        SysUser update = new SysUser();
        update.setId(userId);
        update.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(update);
    }

    public SysUser getUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw BizException.of(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private List<PermissionDtos.UserVO> enrich(List<SysUser> users) {
        if (users.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = users.stream().map(SysUser::getId).toList();
        Map<Long, List<Long>> relations = new HashMap<>();
        for (SysUserRole relation : userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .in(SysUserRole::getUserId, userIds))) {
            relations.computeIfAbsent(relation.getUserId(), k -> new ArrayList<>()).add(relation.getRoleId());
        }
        Set<Long> roleIds = relations.values().stream().flatMap(List::stream).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, SysRole> roleMap = roleIds.isEmpty() ? Map.of()
                : roleMapper.selectBatchIds(roleIds).stream().collect(Collectors.toMap(SysRole::getId, r -> r));
        Map<Long, String> deptNames = deptMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getName, (a, b) -> a));

        List<PermissionDtos.UserVO> result = new ArrayList<>(users.size());
        for (SysUser user : users) {
            PermissionDtos.UserVO vo = new PermissionDtos.UserVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setDeptId(user.getDeptId());
            vo.setDeptName(user.getDeptId() == null ? null : deptNames.get(user.getDeptId()));
            vo.setEmail(user.getEmail());
            vo.setPhone(user.getPhone());
            vo.setStatus(user.getStatus());
            vo.setSuperAdmin(user.getSuperAdmin());
            vo.setLastLoginTime(user.getLastLoginTime());
            vo.setCreateTime(user.getCreateTime());
            List<Long> ids = relations.getOrDefault(user.getId(), List.of());
            vo.setRoleIds(ids);
            vo.setRoleCodes(ids.stream().map(roleMap::get).filter(r -> r != null).map(SysRole::getCode).toList());
            vo.setRoleNames(ids.stream().map(roleMap::get).filter(r -> r != null).map(SysRole::getName).toList());
            result.add(vo);
        }
        return result;
    }

    private void evictUser(Long userId) {
        String tenantCode = TenantContext.currentTenantCode();
        cacheService.evictByPrefix("perm:roles:" + tenantCode + ":" + userId);
        cacheService.evictByPrefix("perm:menu:" + tenantCode + ":" + userId);
        cacheService.evictByPrefix("perm:codes:" + tenantCode + ":" + userId);
        cacheService.evictByPrefix("perm:decision:" + tenantCode);
        cacheService.evictByPrefix("perm:scope:" + tenantCode + ":" + userId);
    }

    public List<String> roleCodes(Long userId) {
        return roleService.roleCodesOfUser(userId);
    }
}
