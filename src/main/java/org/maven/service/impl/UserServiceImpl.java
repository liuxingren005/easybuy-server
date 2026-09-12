package org.maven.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.maven.entity.User;
import org.maven.exception.BusinessException;
import org.maven.mapper.UserMapper;
import org.maven.service.UserService;
import org.maven.util.SecurityUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户服务实现类（easybuy_user 表）
 *
 * 密码加密方式：SM3 国密算法（GB/T 32905-2016）
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public User register(User user) {
        User existing = userMapper.selectByLoginName(user.getLoginName());
        if (existing != null) {
            throw new BusinessException("登录名已存在");
        }

        // 邮箱唯一
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            User emailExist = userMapper.selectByEmail(user.getEmail());
            if (emailExist != null) {
                throw new BusinessException("该邮箱已被注册");
            }
        }

        // 密码 SM3 加密
        user.setPassword(SecurityUtil.sm3(user.getPassword()));

        // 默认前台用户
        if (user.getType() == null) {
            user.setType(0);
        }
        // 默认性别男
        if (user.getSex() == null) {
            user.setSex(1);
        }

        userMapper.insert(user);

        // 清除密码
        user.setPassword(null);
        return user;
    }

    @Override
    public User login(String loginName, String password) {
        User user = userMapper.selectByLoginName(loginName);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // SM3 加密后比对
        if (!SecurityUtil.verify(password, user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        // 清除密码返回
        user.setPassword(null);
        return user;
    }

    @Override
    public User loginByEmail(String email, String password) {
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            throw new BusinessException("该邮箱未注册");
        }
        if (!SecurityUtil.verify(password, user.getPassword())) {
            throw new BusinessException("密码错误");
        }
        user.setPassword(null);
        return user;
    }

    @Override
    public User findByEmail(String email) {
        User user = userMapper.selectByEmail(email);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    @Override
    public PageInfo<User> findPage(Integer pageNum, Integer pageSize, String loginName,
                                   String startTime, String endTime) {
        try (Page<?> page = PageHelper.startPage(pageNum, pageSize)) {
            List<User> list = userMapper.selectPage(loginName, startTime, endTime);
            // 清除密码
            list.forEach(u -> u.setPassword(null));
            return new PageInfo<>(list);
        }
    }

    @Override
    public User findById(Integer id) {
        User user = userMapper.selectById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    @Override
    public void modify(User user) {
        // 管理员不可被降级为普通用户
        if (user.getType() != null && user.getId() != null) {
            User existUser = userMapper.selectById(user.getId());
            if (existUser != null && existUser.getType() != null
                    && existUser.getType() == 1 && user.getType() == 0) {
                throw new BusinessException("无权操作");
            }
        }
        // SM3 加密
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            // 判断是否已是 SM3 密文（64位十六进制），避免二次加密
            if (user.getPassword().length() != 64) {
                user.setPassword(SecurityUtil.sm3(user.getPassword()));
            }
        }
        if (userMapper.update(user) == 0) {
            throw new BusinessException("修改用户失败");
        }
    }

    @Override
    public void modifyProfile(User user) {
        // 个人信息修改：屏蔽密码/类型/登录名/邮箱，防止越权提权、邮箱篡改
        user.setPassword(null);
        user.setType(null);
        user.setLoginName(null);
        user.setEmail(null);    // 邮箱一旦注册不可自行修改
        if (userMapper.update(user) == 0) {
            throw new BusinessException("修改个人信息失败");
        }
    }

    @Override
    public void changePassword(Integer userId, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("新密码长度不能少于6位");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 校验原密码
        if (!SecurityUtil.verify(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        User update = new User();
        update.setId(userId);
        update.setPassword(SecurityUtil.sm3(newPassword));
        if (userMapper.update(update) == 0) {
            throw new BusinessException("修改密码失败");
        }
    }

    @Override
    public void remove(Integer id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getType() != null && user.getType() == 1) {
            throw new BusinessException("管理员账号不可删除");
        }
        if (userMapper.deleteById(id) == 0) {
            throw new BusinessException("删除失败");
        }
    }
}
