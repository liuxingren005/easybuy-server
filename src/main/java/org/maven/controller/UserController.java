package org.maven.controller;

import com.github.pagehelper.PageInfo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.maven.annotation.RequireRole;
import org.maven.common.ResponseResult;
import org.maven.common.Role;
import org.maven.config.JwtProperties;
import org.maven.entity.User;
import org.maven.security.UserContext;
import org.maven.security.UserPrincipal;
import org.maven.service.EmailService;
import org.maven.service.TokenService;
import org.maven.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 用户 Controller（easybuy_user 表）
 *
 * 功能：注册（邮箱验证码）、登录（用户名/邮箱）、修改密码（邮箱验证码）、
 *      分页查询、修改、删除（逻辑删除）
 * 密码：SM3 国密加密
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private final TokenService tokenService;

    private final JwtProperties jwtProperties;

    private final EmailService emailService;

    /**
     * 邮箱正则（登录方式：用户名 或 邮箱）
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * 邮箱验证码场景
     */
    private static final String SCENE_REGISTER = "register";
    private static final String SCENE_CHANGE_PASSWORD = "changePassword";

    /**
     * 发送邮箱验证码
     *
     * POST /user/sendEmailCode
     * Body: { "email": "xxx@xx.com", "scene": "register | changePassword" }
     */
    @PostMapping("/sendEmailCode")
    public ResponseResult sendEmailCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String scene = body.get("scene");

        if (email == null || email.isBlank()) {
            return ResponseResult.error("邮箱不能为空");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return ResponseResult.error("邮箱格式不正确");
        }
        if (scene == null || scene.isBlank()) {
            scene = SCENE_REGISTER;
        }

        // 注册
        if (SCENE_REGISTER.equals(scene)) {
            User exist = userService.findByEmail(email);
            if (exist != null) {
                return ResponseResult.error("该邮箱已被注册");
            }
        }
        // 修改密码
        if (SCENE_CHANGE_PASSWORD.equals(scene)) {
            User exist = userService.findByEmail(email);
            if (exist == null) {
                return ResponseResult.error("该邮箱未注册");
            }
        }

        String code = emailService.sendCode(email, scene);
        return ResponseResult.success("验证码已发送（有效期 5 分钟）")
                .put("captchaCode", code);
    }

    /**
     * 用户注册
     *
     * POST /user/register
     * Body: { "loginName": "", "userName": "", "password": "", "email": "", "mobile": "", "emailCode": "" }
     * 邮箱验证码场景：register；登录名/邮箱唯一；密码 SM3 加密
     */
    @PostMapping("/register")
    public ResponseResult register(@RequestBody Map<String, String> body) {
        String loginName = body.get("loginName");
        String userName = body.get("userName");
        String password = body.get("password");
        String email = body.get("email");
        String mobile = body.get("mobile");
        String emailCode = body.get("emailCode");

        if (loginName == null || loginName.isBlank()) {
            return ResponseResult.error("登录名不能为空");
        }
        if (password == null || password.isBlank()) {
            return ResponseResult.error("密码不能为空");
        }
        if (password.length() < 6) {
            return ResponseResult.error("密码长度不能少于6位");
        }
        if (email == null || email.isBlank()) {
            return ResponseResult.error("邮箱不能为空");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return ResponseResult.error("邮箱格式不正确");
        }
        if (emailCode == null || emailCode.isBlank()) {
            return ResponseResult.error("请输入邮箱验证码");
        }
        emailService.verifyCode(email, SCENE_REGISTER, emailCode);

        User user = new User();
        user.setLoginName(loginName);
        user.setUserName(userName);
        user.setPassword(password);
        user.setEmail(email);
        user.setMobile(mobile);

        User registerUser = userService.register(user);
        return ResponseResult.success().put("data", registerUser);
    }

    /**
     * 用户登录（用户名 或 邮箱）
     *
     * POST /user/login
     * Body: { "loginName": "", "password": "" }
     */
    @PostMapping("/login")
    public ResponseResult login(@RequestBody User user) {
        if (user.getLoginName() == null || user.getLoginName().isBlank()) {
            return ResponseResult.error("账号不能为空");
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            return ResponseResult.error("密码不能为空");
        }

        User loginUser;
        String account = user.getLoginName().trim();
        if (EMAIL_PATTERN.matcher(account).matches()) {
            // 邮箱登录
            loginUser = userService.loginByEmail(account, user.getPassword());
        } else {
            // 用户名登录
            loginUser = userService.login(account, user.getPassword());
        }
        // 生成 JWT 令牌并写入 Redis 会话
        String token = tokenService.create(loginUser);
        return Objects.requireNonNull(ResponseResult.success().put("data", loginUser))
                .put("token", token);
    }

    /**
     * 分页查询用户列表
     *
     * GET /user/page?pageNum=1&pageSize=5&loginName=
     */
    @RequireRole(Role.ADMIN)
    @GetMapping("/page")
    public ResponseResult findPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String loginName,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {

        PageInfo<User> pageInfo = userService.findPage(pageNum, pageSize, loginName, startTime, endTime);
        return ResponseResult.success().put("page", pageInfo);
    }

    /**
     * 根据ID查询用户
     *
     * GET /user/{id}
     */
    @GetMapping("/{id}")
    public ResponseResult findById(@PathVariable Integer id) {
        User user = userService.findById(id);
        if (user == null) {
            return ResponseResult.error("用户不存在");
        }
        return ResponseResult.success().put("data", user);
    }

    /**
     * 修改用户
     *
     * PUT /user
     * Body: { "id": 1, "userName": "", "password": "", "email": "", "mobile": "", "sex": 1, "type": 0 }
     */
    @RequireRole(Role.ADMIN)
    @PutMapping
    public ResponseResult update(@RequestBody User user) {
        if (user.getId() == null) {
            return ResponseResult.error("ID不能为空");
        }
        userService.modify(user);
        return ResponseResult.success();
    }

    /**
     * 逻辑删除用户
     *
     * DELETE /user/{id}
     */
    @RequireRole(Role.ADMIN)
    @DeleteMapping("/{id}")
    public ResponseResult delete(@PathVariable Integer id) {
        userService.remove(id);
        return ResponseResult.success();
    }

    /**
     * 退出登录（删除 Redis 会话）
     *
     * POST /user/logout
     */
    @PostMapping("/logout")
    public ResponseResult logout(HttpServletRequest request) {
        String token = tokenService.resolve(request.getHeader(jwtProperties.getHeader()));
        tokenService.remove(token);
        return ResponseResult.success();
    }

    /**
     * 获取当前登录用户信息
     *
     * GET /user/current
     */
    @GetMapping("/current")
    public ResponseResult current() {
        UserPrincipal userPrincipal = UserContext.get();
        User user = userService.findById(userPrincipal.getUserId());
        return ResponseResult.success().put("data", user);
    }

    /**
     * 修改个人基础信息（当前登录用户）
     *
     * PUT /user/profile
     * Body: { "userName": "", "sex": 1, "email": "", "mobile": "", "identityCode": "" }
     */
    @PutMapping("/profile")
    public ResponseResult updateProfile(@RequestBody User user) {
        // 使用当前登录用户 ID，防止修改他人信息
        user.setId(UserContext.getUserId());
        userService.modifyProfile(user);
        return ResponseResult.success();
    }

    /**
     * 修改个人密码（当前登录用户，邮箱验证码）
     *
     * PUT /user/password
     * Body: { "oldPassword": "", "newPassword": "", "emailCode": "" }
     * 邮箱验证码场景：changePassword，收件人：当前登录用户邮箱
     */
    @PutMapping("/password")
    public ResponseResult changePassword(@RequestBody Map<String, String> body) {
        Integer userId = UserContext.getUserId();
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        String emailCode = body.get("emailCode");

        // 邮箱验证码
        if (emailCode == null || emailCode.isBlank()) {
            return ResponseResult.error("请输入邮箱验证码");
        }
        User currentUser = userService.findById(userId);
        if (currentUser == null || currentUser.getEmail() == null) {
            return ResponseResult.error("用户信息异常，无法完成邮箱验证");
        }

        emailService.verifyCode(currentUser.getEmail(),SCENE_CHANGE_PASSWORD, emailCode);
        userService.changePassword(userId, oldPassword, newPassword);
        return ResponseResult.success();
    }
}
