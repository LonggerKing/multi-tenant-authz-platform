package com.enterprise.platform.common.security;

import com.enterprise.platform.common.exception.BizException;
import com.enterprise.platform.common.exception.ErrorCode;

/**
 * 当前登录用户上下文（与 Spring Security 上下文并行维护，便于跨模块轻量读取）。
 */
public final class UserContext {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static boolean isAuthenticated() {
        return HOLDER.get() != null;
    }

    public static CurrentUser require() {
        CurrentUser user = HOLDER.get();
        if (user == null) {
            throw BizException.of(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    public static Long currentUserId() {
        return require().getUserId();
    }

    public static Long currentDeptId() {
        return require().getDeptId();
    }

    public static boolean isSuperAdmin() {
        CurrentUser user = HOLDER.get();
        return user != null && user.isSuperAdmin();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
