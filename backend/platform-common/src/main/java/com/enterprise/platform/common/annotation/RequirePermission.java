package com.enterprise.platform.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明式权限点校验，配合 {@code @PreAuthorize("@perm.hasAny(...)")} 使用。
 * <p>权限点形如 {@code system:user:list}，对应 Casbin 中的 obj。</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequirePermission {

    /** 需要的权限点，多个时满足任意一个即通过。 */
    String[] value();

    /** 多个权限点是否必须全部满足。 */
    boolean requireAll() default false;
}
