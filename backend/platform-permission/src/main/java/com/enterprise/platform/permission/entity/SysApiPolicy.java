package com.enterprise.platform.permission.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enterprise.platform.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ABAC 条件策略：在 RBAC 放行之后，对命中的资源追加属性条件约束。
 * <p>示例：只允许工作时间访问、只允许内网 IP 访问、只允许操作本部门单据。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_api_policy")
public class SysApiPolicy extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String name;

    /** 资源匹配模式，keyMatch2 语法，如 /api/demo/orders 或 system:user:*。 */
    private String resourcePattern;

    /** HTTP 方法，* 表示全部。 */
    private String httpMethod;

    /** 条件表达式（SpEL），如 hour >= 9 && hour < 18；为空表示无附加条件。 */
    private String conditionExpr;

    /** allow / deny。 */
    private String effect;

    /** 1 启用，0 停用。 */
    private Integer status;

    /** 数值越小优先级越高。 */
    private Integer priority;

    private String remark;
}
