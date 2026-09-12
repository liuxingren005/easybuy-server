package org.maven.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录用户（JWT）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPrincipal {

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 登录名
     */
    private String loginName;

    /**
     * 类型（1:后台管理员 0:前台用户）
     */
    private Integer type;
}
