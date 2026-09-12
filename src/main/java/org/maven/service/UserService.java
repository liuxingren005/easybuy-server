package org.maven.service;

import com.github.pagehelper.PageInfo;
import org.maven.entity.User;

/**
 * 用户服务接口（easybuy_user 表）
 *
 * 密码加密方式：SM3 国密算法（GB/T 32905-2016）
 */
public interface UserService {

    /**
     * 用户注册
     */
    User register(User user);

    /**
     * 用户登录
     */
    User login(String loginName, String password);

    /**
     * 用户登录（邮箱）
     */
    User loginByEmail(String email, String password);

    /**
     * 根据邮箱查询用户
     */
    User findByEmail(String email);

    /**
     * 分页查询用户列表
     */
    PageInfo<User> findPage(Integer pageNum, Integer pageSize, String loginName,
                            String startTime, String endTime);

    /**
     * 根据ID查询用户
     */
    User findById(Integer id);

    /**
     * 修改用户
     */
    void modify(User user);

    /**
     * 修改个人基础信息
     */
    void modifyProfile(User user);

    /**
     * 修改个人密码
     */
    void changePassword(Integer userId, String oldPassword, String newPassword);

    /**
     * 逻辑删除用户
     */
    void remove(Integer id);
}
