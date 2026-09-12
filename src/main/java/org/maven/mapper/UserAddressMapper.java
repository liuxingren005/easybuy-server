package org.maven.mapper;

import io.lettuce.core.dynamic.annotation.Param;
import org.maven.entity.UserAddress;

import java.util.List;

/**
 * 用户地址数据访问接口
 */
public interface UserAddressMapper {

    /**
     * 根据用户ID查询地址列表
     */
    List<UserAddress> selectByUserId(Integer userId);

    /**
     * 统计用户地址
     */
    int countByUserId(Integer userId);

    /**
     * 根据ID查询
     */
    UserAddress selectById(Integer id);

    /**
     * 查询用户默认地址
     */
    UserAddress selectDefaultByUserId(Integer userId);

    /**
     * 新增
     */
    int insert(UserAddress userAddress);

    /**
     * 修改
     */
    int update(UserAddress userAddress);

    /**
     * 逻辑删除
     */
    int deleteById(Integer id);

    /**
     * 取消用户所有默认地址
     */
    int cancelDefaultByUserId(@Param("userId") Integer userId);
}
