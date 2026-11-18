package com.enterprise.platform.tenant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enterprise.platform.common.model.BaseEntity;
import com.enterprise.platform.common.tenant.IsolationMode;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 租户（平台级实体，不参与 tenant_id 过滤）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant")
public class SysTenant extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户编码，全局唯一，登录时使用。 */
    private String code;

    private String name;

    /** SHARED / SCHEMA。 */
    private IsolationMode isolationMode;

    /** 独立 Schema 名称，仅 SCHEMA 模式使用。 */
    private String schemaName;

    /** 1 启用，0 停用。 */
    private Integer status;

    /** 到期时间，null 表示永久。 */
    private LocalDateTime expireAt;

    private String contactName;

    private String contactPhone;

    private Integer maxUsers;

    private String remark;
}
