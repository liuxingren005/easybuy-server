package org.maven.annotation;

import org.maven.common.Role;

import java.lang.annotation.*;

/**
 * 角色权限注解（层级式）
 * <p>
 * 方法级优先于类级 示例：@RequireRole(Role.ADMIN)
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    /**
     * 所需最低角色
     */
    Role value();
}
