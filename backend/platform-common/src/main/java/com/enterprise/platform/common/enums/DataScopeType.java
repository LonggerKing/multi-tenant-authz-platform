package com.enterprise.platform.common.enums;

/**
 * 数据权限范围。
 */
public enum DataScopeType {

    /** 全部数据。 */
    ALL,

    /** 本部门。 */
    DEPT,

    /** 本部门及下级部门。 */
    DEPT_AND_CHILD,

    /** 仅本人创建的数据。 */
    SELF,

    /** 自定义部门集合。 */
    CUSTOM
}
