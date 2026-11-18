package com.enterprise.platform.permission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.platform.common.exception.BizException;
import com.enterprise.platform.common.exception.ErrorCode;
import com.enterprise.platform.common.tenant.TenantContext;
import com.enterprise.platform.permission.dto.PermissionDtos;
import com.enterprise.platform.permission.entity.SysDept;
import com.enterprise.platform.permission.entity.SysUser;
import com.enterprise.platform.permission.mapper.SysDeptMapper;
import com.enterprise.platform.permission.mapper.SysUserMapper;
import com.enterprise.platform.permission.scopedata.DataScopeService;
import com.enterprise.platform.permission.support.TreeBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 组织架构（部门树）。path 字段维护物化路径，支撑 DEPT_AND_CHILD 数据范围的高效查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeptService {

    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;
    private final DataScopeService dataScopeService;

    public List<PermissionDtos.DeptNode> tree() {
        return TreeBuilder.buildDeptTree(deptMapper.selectList(
                new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getSort)));
    }

    public List<SysDept> listAll() {
        return deptMapper.selectList(new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getSort));
    }

    @Transactional(rollbackFor = Exception.class)
    public SysDept create(PermissionDtos.DeptSaveRequest request) {
        SysDept dept = new SysDept();
        dept.setTenantId(TenantContext.currentTenantId());
        dept.setParentId(request.getParentId() == null ? 0L : request.getParentId());
        dept.setName(request.getName());
        dept.setSort(request.getSort());
        dept.setLeader(request.getLeader());
        dept.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        deptMapper.insert(dept);
        dept.setPath(buildPath(dept.getParentId(), dept.getId()));
        deptMapper.updateById(dept);
        return dept;
    }

    @Transactional(rollbackFor = Exception.class)
    public SysDept update(Long id, PermissionDtos.DeptSaveRequest request) {
        SysDept dept = getDept(id);
        dept.setName(request.getName());
        dept.setSort(request.getSort());
        dept.setLeader(request.getLeader());
        dept.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        deptMapper.updateById(dept);
        dataScopeService.evictCurrentTenant();
        return dept;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long children = deptMapper.selectCount(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getParentId, id));
        if (children != null && children > 0) {
            throw BizException.of(ErrorCode.BAD_REQUEST, "存在下级部门，无法删除");
        }
        Long users = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeptId, id));
        if (users != null && users > 0) {
            throw BizException.of(ErrorCode.BAD_REQUEST, "部门下存在用户，无法删除");
        }
        deptMapper.deleteById(id);
        dataScopeService.evictCurrentTenant();
    }

    public SysDept getDept(Long id) {
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw BizException.of(ErrorCode.DEPT_NOT_FOUND);
        }
        return dept;
    }

    private String buildPath(Long parentId, Long id) {
        String parentPath = "/";
        if (parentId != null && parentId != 0) {
            SysDept parent = deptMapper.selectById(parentId);
            if (parent != null && StringUtils.hasText(parent.getPath())) {
                parentPath = parent.getPath();
            }
        }
        return parentPath.endsWith("/") ? parentPath + id + "/" : parentPath + "/" + id + "/";
    }
}
