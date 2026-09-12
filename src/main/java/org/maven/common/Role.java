package org.maven.common;

import lombok.Getter;

/**
 * 角色枚举（层级式）
 *
 * GUEST ⊂ USER ⊂ ADMIN
 */
@Getter
public enum Role {

    /**
     * 游客（未登录）
     */
    GUEST(-1),

    /**
     * 普通用户（type=0）
     */
    USER(0),

    /**
     * 管理员（type=1）
     */
    ADMIN(1);

    /**
     * 角色等级
     */
    private final int level;

    Role(int level) {
        this.level = level;
    }

    /**
     * 当前角色是否满足所需角色
     */
    public boolean satisfy(Role requireRole) {
        return this.level >= requireRole.level;
    }

    /**
     * 临时映射：由数据库 type 直接映射（角色表）
     */
    public static Role fromType(Integer type) {
        if (type == null) {
            return GUEST;
        }
        for (Role role : values()) {
            if (role.level == type) {
                return role;
            }
        }
        return GUEST;
    }
}
