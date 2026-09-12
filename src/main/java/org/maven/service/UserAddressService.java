package org.maven.service;

import org.maven.entity.UserAddress;

import java.util.List;

/**
 * 用户地址业务接口
 */
public interface UserAddressService {

    /**
     * 根据用户ID查询地址列表
     */
    List<UserAddress> findByUserId(Integer userId);

    /**
     * 根据ID查询
     */
    UserAddress findById(Integer id);

    /**
     * 查询用户默认地址
     */
    UserAddress findDefaultByUserId(Integer userId);

    /**
     * 新增
     */
    void add(UserAddress userAddress);

    /**
     * 修改
     */
    void modify(UserAddress userAddress);

    /**
     * 逻辑删除
     */
    void removeById(Integer id);

    /**
     * 设置默认地址
     */
    void setDefault(Integer id, Integer userId);
}
