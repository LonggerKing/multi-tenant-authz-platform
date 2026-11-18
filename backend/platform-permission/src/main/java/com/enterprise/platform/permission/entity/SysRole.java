package com.enterprise.platform.permission.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enterprise.platform.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色。dataScope 决定行级数据可见范围。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String code;

    private String name;

    /** ALL / DEPT / DEPT_AND_CHILD / SELF / CUSTOM */
    private String dataScope;

    /** 自定义数据范围部门 ID，逗号分隔（dataScope=CUSTOM 时生效）。 */
    private String customDeptIds;

    /** 1 启用，0 停用。 */
    private Integer status;

    /** 内置角色不可删除。 */
    private Boolean builtin;

    private String remark;
}
