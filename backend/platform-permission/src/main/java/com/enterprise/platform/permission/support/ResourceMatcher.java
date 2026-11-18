package com.enterprise.platform.permission.support;

import java.util.regex.Pattern;

/**
 * 资源匹配器：兼容 `*` 通配与 keyMatch2 风格的路径参数（:id / {id}）。
 * <p>用于 ABAC 策略的资源命中判断，避免依赖 Casbin 内部工具类。</p>
 */
public final class ResourceMatcher {

    private ResourceMatcher() {
    }

    public static boolean matches(String obj, String pattern) {
        if (obj == null || pattern == null) {
            return false;
        }
        if ("*".equals(pattern) || "*".equals(obj)) {
            return true;
        }
        if (pattern.equals(obj)) {
            return true;
        }
        if (pattern.endsWith("*") && pattern.lastIndexOf('*') == pattern.length() - 1) {
            return obj.startsWith(pattern.substring(0, pattern.length() - 1));
        }
        if (pattern.indexOf(':') >= 0 || pattern.indexOf('{') >= 0) {
            String regex = pattern.replaceAll(":[^/]+", "[^/]+")
                    .replaceAll("\\{[^/]+}", "[^/]+");
            try {
                return Pattern.compile(regex).matcher(obj).matches();
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
}
