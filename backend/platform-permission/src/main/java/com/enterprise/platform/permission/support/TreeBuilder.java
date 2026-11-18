package com.enterprise.platform.permission.support;

import com.enterprise.platform.permission.dto.PermissionDtos;
import com.enterprise.platform.permission.entity.SysDept;
import com.enterprise.platform.permission.entity.SysMenu;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 树结构工具。
 */
public final class TreeBuilder {

    private TreeBuilder() {
    }

    public static List<PermissionDtos.MenuNode> buildMenuTree(List<SysMenu> menus) {
        Map<Long, PermissionDtos.MenuNode> nodes = new LinkedHashMap<>();
        List<SysMenu> sorted = menus.stream()
                .sorted(Comparator.comparing(m -> m.getSort() == null ? 0 : m.getSort()))
                .toList();
        for (SysMenu menu : sorted) {
            PermissionDtos.MenuNode node = new PermissionDtos.MenuNode();
            BeanUtils.copyProperties(menu, node, "children");
            node.setChildren(new ArrayList<>());
            nodes.put(menu.getId(), node);
        }
        List<PermissionDtos.MenuNode> roots = new ArrayList<>();
        for (SysMenu menu : sorted) {
            PermissionDtos.MenuNode node = nodes.get(menu.getId());
            Long parentId = menu.getParentId();
            if (parentId == null || parentId == 0 || !nodes.containsKey(parentId)) {
                roots.add(node);
            } else {
                nodes.get(parentId).getChildren().add(node);
            }
        }
        return roots;
    }

    public static List<PermissionDtos.DeptNode> buildDeptTree(List<SysDept> depts) {
        Map<Long, PermissionDtos.DeptNode> nodes = new LinkedHashMap<>();
        List<SysDept> sorted = depts.stream()
                .sorted(Comparator.comparing(d -> d.getSort() == null ? 0 : d.getSort()))
                .toList();
        for (SysDept dept : sorted) {
            PermissionDtos.DeptNode node = new PermissionDtos.DeptNode();
            BeanUtils.copyProperties(dept, node, "children");
            node.setChildren(new ArrayList<>());
            nodes.put(dept.getId(), node);
        }
        List<PermissionDtos.DeptNode> roots = new ArrayList<>();
        for (SysDept dept : sorted) {
            PermissionDtos.DeptNode node = nodes.get(dept.getId());
            Long parentId = dept.getParentId();
            if (parentId == null || parentId == 0 || !nodes.containsKey(parentId)) {
                roots.add(node);
            } else {
                nodes.get(parentId).getChildren().add(node);
            }
        }
        return roots;
    }

    public static List<Long> collectIds(List<PermissionDtos.MenuNode> nodes) {
        return nodes.stream().map(PermissionDtos.MenuNode::getId).collect(Collectors.toList());
    }
}
