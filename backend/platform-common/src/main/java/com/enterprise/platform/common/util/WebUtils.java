package com.enterprise.platform.common.util;

import com.enterprise.platform.common.constant.PlatformConstants;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Web 请求工具。
 */
public final class WebUtils {

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"
    };

    private WebUtils() {
    }

    public static String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        for (String header : IP_HEADERS) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value)) {
                int idx = value.indexOf(',');
                return idx > 0 ? value.substring(0, idx).trim() : value.trim();
            }
        }
        return request.getRemoteAddr();
    }

    public static String tenantHeader(HttpServletRequest request) {
        return request.getHeader(PlatformConstants.HEADER_TENANT);
    }

    public static String bearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }

    public static boolean isAjax(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith);
    }
}
