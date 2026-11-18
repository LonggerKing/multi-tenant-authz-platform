package com.enterprise.platform.common.tenant;

/**
 * 租户数据隔离模式。
 */
public enum IsolationMode {

    /**
     * 共享表 + tenant_id 列，逻辑隔离。绝大多数租户使用该模式，成本最低。
     */
    SHARED,

    /**
     * 独立 Schema（共享库），物理隔离。面向强合规/大客户租户。
     */
    SCHEMA
}
