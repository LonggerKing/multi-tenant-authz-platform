package com.enterprise.platform.permission.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enterprise.platform.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部门（组织架构树）。path 形如 "/1/10/1001/"，用于子树范围查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDept extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long parentId;

    private String name;

    private String path;

    private Integer sort;

    private String leader;

    /** 1 启用，0 停用。 */
    private Integer status;
}
