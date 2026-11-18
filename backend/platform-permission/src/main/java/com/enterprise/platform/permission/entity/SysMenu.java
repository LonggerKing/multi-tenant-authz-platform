package com.enterprise.platform.permission.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enterprise.platform.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单/资源。
 * <p>type: DIR 目录、MENU 页面、BUTTON 按钮（权限点）、API 接口资源。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long parentId;

    /** 展示名称。 */
    private String name;

    /** 权限点编码，如 system:user:list。 */
    private String code;

    /** DIR / MENU / BUTTON / API。 */
    private String type;

    /** 前端路由（MENU）或接口路径模式（API），支持 keyMatch2 通配。 */
    private String path;

    /** 前端组件路径。 */
    private String component;

    private String icon;

    /** API 资源的 HTTP 方法，* 表示全部。 */
    private String httpMethod;

    private Integer sort;

    /** 1 显示，0 隐藏。 */
    private Integer visible;

    /** 1 启用，0 停用。 */
    private Integer status;
}
