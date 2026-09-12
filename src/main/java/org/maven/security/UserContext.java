package org.maven.security;

/**
 * 当前登录用户上下文
 * 线程封装（ThreadLocalMap）
 */
public final class UserContext {

    private static final ThreadLocal<UserPrincipal> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    // 设置当前登录用户
    public static void set(UserPrincipal userPrincipal) {
        HOLDER.set(userPrincipal);
    }

    // 获取当前登录用户对象
    public static UserPrincipal get() {
        return HOLDER.get();
    }

    // 获取当前登录用户ID
    public static Integer getUserId() {
        UserPrincipal userPrincipal = HOLDER.get();
        return userPrincipal == null ?
                null : userPrincipal.getUserId();
    }

    // 清除当前用户上下文
    public static void clear() {
        HOLDER.remove();
    }
}
