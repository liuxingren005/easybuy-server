package org.maven.entity;

import lombok.Data;

/**
 * 用户实体类（easybuy_user 表）
 */
@Data
public class User {

    /**
     * 主键
     */
    private Integer id;

    /**
     * 登录名（唯一约束）
     */
    private String loginName;

    /**
     * 用户名（显示名）
     */
    private String userName;

    /**
     * 密码（SM3 国密加密）
     */
    private String password;

    /**
     * 性别（1:男 0:女）
     */
    private Integer sex;

    /**
     * 身份证号
     */
    private String identityCode;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机
     */
    private String mobile;

    /**
     * 类型（1:后台管理员 0:前台用户）
     */
    private Integer type;

    /**
     * 是否删除（1:已删除 0:未删除）
     */
    private Integer isDelete;
}
