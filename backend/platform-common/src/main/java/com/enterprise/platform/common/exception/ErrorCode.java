package com.enterprise.platform.common.exception;

import lombok.Getter;

/**
 * 平台错误码。
 * <p>约定：40xxx 表示请求/业务问题，401xx 表示认证，403xx 表示授权。</p>
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "OK"),

    BAD_REQUEST(40000, "请求参数不合法"),
    NOT_FOUND(40400, "资源不存在"),
    CONFLICT(40900, "资源已存在"),
    INTERNAL_ERROR(50000, "系统内部错误"),

    TENANT_NOT_FOUND(40001, "租户不存在"),
    TENANT_DISABLED(40002, "租户已停用"),
    TENANT_EXPIRED(40003, "租户已过期"),
    TENANT_MISSING(40004, "缺少租户上下文"),

    UNAUTHORIZED(40100, "未认证或凭证无效"),
    LOGIN_FAILED(40101, "用户名或密码错误"),
    TOKEN_EXPIRED(40102, "凭证已过期"),
    TOKEN_INVALID(40103, "凭证无效"),
    ACCOUNT_DISABLED(40104, "账号已停用"),

    FORBIDDEN(40300, "无访问权限"),
    PERMISSION_DENIED(40301, "缺少所需权限"),

    ROLE_NOT_FOUND(40401, "角色不存在"),
    USER_NOT_FOUND(40402, "用户不存在"),
    MENU_NOT_FOUND(40403, "菜单不存在"),
    DEPT_NOT_FOUND(40404, "部门不存在"),

    ISOLATION_NOT_SUPPORTED(40010, "当前环境不支持该隔离模式"),
    SCHEMA_INIT_FAILED(40011, "租户 Schema 初始化失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
