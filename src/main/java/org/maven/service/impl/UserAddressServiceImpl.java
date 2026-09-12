package org.maven.service.impl;

import org.maven.entity.UserAddress;
import org.maven.exception.BusinessException;
import org.maven.mapper.UserAddressMapper;
import org.maven.service.UserAddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 用户地址业务实现类
 */
@Service
public class UserAddressServiceImpl implements UserAddressService {

    private final UserAddressMapper userAddressMapper;

    public UserAddressServiceImpl(UserAddressMapper userAddressMapper) {
        this.userAddressMapper = userAddressMapper;
    }

    @Override
    public List<UserAddress> findByUserId(Integer userId) {
        return userAddressMapper.selectByUserId(userId);
    }

    @Override
    public UserAddress findById(Integer id) {
        return userAddressMapper.selectById(id);
    }

    @Override
    public UserAddress findDefaultByUserId(Integer userId) {
        return userAddressMapper.selectDefaultByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(UserAddress userAddress) {
        // 地址数量上限（最多16个）
        int count = userAddressMapper.countByUserId(userAddress.getUserId());
        if (count >= 16) {
            throw new BusinessException("地址数量已达上限（最多16个）");
        }
        userAddress.setCreateTime(new Date());
        // 如果设为默认，先取消该用户其他默认地址
        if (userAddress.getIsDefault() != null && userAddress.getIsDefault() == 1) {
            userAddressMapper.cancelDefaultByUserId(userAddress.getUserId());
        }
        if (userAddressMapper.insert(userAddress) == 0) {
            throw new BusinessException("新增地址失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void modify(UserAddress userAddress) {
        // 如果设为默认，先取消该用户其他默认地址
        if (userAddress.getIsDefault() != null && userAddress.getIsDefault() == 1) {
            userAddressMapper.cancelDefaultByUserId(userAddress.getUserId());
        }
        if (userAddressMapper.update(userAddress) == 0) {
            throw new BusinessException("修改地址失败");
        }
    }

    @Override
    public void removeById(Integer id) {
        if (userAddressMapper.deleteById(id) == 0) {
            throw new BusinessException("删除地址失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Integer id, Integer userId) {
        userAddressMapper.cancelDefaultByUserId(userId);
        UserAddress address = new UserAddress();
        address.setId(id);
        address.setIsDefault(1);
        if (userAddressMapper.update(address) == 0) {
            throw new BusinessException("设置默认地址失败");
        }
    }
}
