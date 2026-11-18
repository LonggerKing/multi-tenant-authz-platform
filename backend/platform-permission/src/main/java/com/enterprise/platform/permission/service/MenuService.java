package com.enterprise.platform.permission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.platform.common.exception.BizException;
import com.enterprise.platform.common.exception.ErrorCode;
import com.enterprise.platform.common.tenant.TenantContext;
import com.enterprise.platform.permission.cache.PermissionCacheService;
import com.enterprise.platform.permission.dto.PermissionDtos;
import com.enterprise.platform.permission.entity.SysMenu;
import com.enterprise.platform.permission.mapper.SysMenuMapper;
import com.enterprise.platform.permission.support.TreeBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 菜单与资源（页面 / 按钮权限点 / 接口资源）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuService {

    private final SysMenuMapper menuMapper;
    private final PermissionCacheService cacheService;

    public List<PermissionDtos.MenuNode> tree() {
        List<SysMenu> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSort));
        return TreeBuilder.buildMenuTree(menus);
    }

    public List<SysMenu> listAll() {
        return menuMapper.selectList(new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSort));
    }

    @Transactional(rollbackFor = Exception.class)
    public SysMenu create(PermissionDtos.MenuSaveRequest request) {
        SysMenu menu = new SysMenu();
        BeanUtils.copyProperties(request, menu);
        menu.setTenantId(TenantContext.currentTenantId());
        menu.setParentId(request.getParentId() == null ? 0L : request.getParentId());
        menuMapper.insert(menu);
        evict();
        return menu;
    }

    @Transactional(rollbackFor = Exception.class)
    public SysMenu update(Long id, PermissionDtos.MenuSaveRequest request) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw BizException.of(ErrorCode.MENU_NOT_FOUND);
        }
        BeanUtils.copyProperties(request, menu, "id", "tenantId");
        menuMapper.updateById(menu);
        evict();
        return menu;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long children = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, id));
        if (children != null && children > 0) {
            throw BizException.of(ErrorCode.BAD_REQUEST, "存在子节点，无法删除");
        }
        menuMapper.deleteById(id);
        evict();
    }

    private void evict() {
        cacheService.evictByPrefix("perm:menu:" + TenantContext.currentTenantCode());
        cacheService.evictByPrefix("perm:codes:" + TenantContext.currentTenantCode());
        cacheService.evictByPrefix("perm:decision:" + TenantContext.currentTenantCode());
    }
}
