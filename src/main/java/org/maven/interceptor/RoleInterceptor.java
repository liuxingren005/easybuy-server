package org.maven.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.maven.annotation.RequireRole;
import org.maven.common.ResponseResult;
import org.maven.common.Role;
import org.maven.security.UserContext;
import org.maven.security.UserPrincipal;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 角色权限拦截器：识别 @RequireRole 注解
 */
@Component
public class RoleInterceptor implements HandlerInterceptor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // CORS 预检、非控制器（静态资源、WebSocket...）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())
                || !(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 方法级注解优先，其次类级注解
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        // 无注解 = 不限制角色
        if (requireRole == null) {
            return true;
        }

        // 命中注解：必须已登录
        UserPrincipal userPrincipal = UserContext.get();
        if (userPrincipal == null) {
            write(response, HttpServletResponse.SC_UNAUTHORIZED, "未登录或登录已过期");
            return false;
        }

        // 角色匹配
        Role currentRole = Role.fromType(userPrincipal.getType());
        if (!currentRole.satisfy(requireRole.value())) {
            write(response, HttpServletResponse.SC_FORBIDDEN, "权限不足，拒绝访问");
            return false;
        }
        return true;
    }

    /**
     * 写出 JSON 错误
     */
    private void write(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().write(
                MAPPER.writeValueAsString(new ResponseResult(status, message))
                        .getBytes(StandardCharsets.UTF_8));
    }
}
