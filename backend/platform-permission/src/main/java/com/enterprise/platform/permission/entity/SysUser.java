package com.enterprise.platform.permission.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enterprise.platform.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户。tenant_id 由租户拦截器统一注入与过滤。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long deptId;

    private String username;

    private String nickname;

    private String password;

    private String email;

    private String phone;

    private String avatar;

    /** 1 启用，0 停用。 */
    private Integer status;

    /** 平台超管：可跨租户运维。 */
    private Boolean superAdmin;

    private LocalDateTime lastLoginTime;
}
