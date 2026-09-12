package org.maven.mapper;

import org.maven.entity.User;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 用户 Mapper 接口（easybuy_user 表）
 */
public interface UserMapper {

    /**
     * 分页条件查询
     */
    List<User> selectPage(@Param("loginName") String loginName,
                          @Param("startTime") String startTime,
                          @Param("endTime") String endTime);

    /** 根据ID查询 */
    User selectById(@Param("id") Integer id);

    /** 根据登录名查询（登录、注册） */
    User selectByLoginName(@Param("loginName") String loginName);

    /** 根据邮箱查询 */
    User selectByEmail(@Param("email") String email);

    /** 新增 */
    int insert(User user);

    /** 修改 */
    int update(User user);

    /** 逻辑删除 */
    int deleteById(@Param("id") Integer id);
}
