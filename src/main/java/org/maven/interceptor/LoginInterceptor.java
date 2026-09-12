package org.maven.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.maven.security.UserPrincipal;
import org.maven.common.ResponseResult;
import org.maven.config.AuthProperties;
import org.maven.config.JwtProperties;
import org.maven.service.TokenService;
import org.maven.security.UserContext;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 登录拦截器：校验 JWT + Redis 会话，写入登录用户上下文
 */
@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TokenService tokenService;
    private final JwtProperties jwtProperties;
    private final AuthProperties authProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // CORS 预检
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 校验令牌（白名单接口携带令牌时同样解析身份）
        String token = tokenService.resolve(request.getHeader(jwtProperties.getHeader()));
        // 会话延期：快过期时换发新令牌，经响应头下发给前端
        String newToken = tokenService.refreshSession(token); // 检查是否需要“续命”，会话延期
        if (newToken != null) {
            token = newToken;
            response.setHeader(jwtProperties.getHeader(), jwtProperties.getPrefix() + newToken);
        }

        UserPrincipal userPrincipal = tokenService.verify(token); // 校验令牌并返回登录用户

        // 白名单：已登录则写入上下文
        if (isWhiteList(request.getMethod(), request.getRequestURI())) {
            // 携带无效Token时返回401
            if (token != null && !token.isBlank() && userPrincipal == null) { // 无法解析
                writeUnauthorized(response, token);
                return false;
            }
            if (userPrincipal != null) {
                UserContext.set(userPrincipal);
            }
            return true;
        }
        // 未认证时返回401
        if (userPrincipal == null) {
            writeUnauthorized(response, token);
            return false;
        }
        UserContext.set(userPrincipal);
        return true;
    }

    /**
     * 整个请求完成后
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 请求结束清理线程上下文，避免线程复用串号
        UserContext.clear();
    }

    /**
     * 401 提示语：被踢下线与登录过期
     * {"code": 401, "message": "您的账号已在其他设备登录，您已被迫下线", "kicked": true}
     */
    private void writeUnauthorized(HttpServletResponse response, String token) throws Exception {
        boolean kicked = tokenService.isKicked(token);
        ResponseResult body = new ResponseResult(HttpServletResponse.SC_UNAUTHORIZED,
                kicked ? "您的账号已在其他设备登录，您已被迫下线" : "登录已过期，请重新登录");
        // 被迫下线/登录过期
        body.put("kicked", kicked);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().write(
                MAPPER.writeValueAsString(body).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 写出 JSON 错误
     */
    /* private void write(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().write(
                MAPPER.writeValueAsString(new ResponseResult(status, message))
                        .getBytes(StandardCharsets.UTF_8));
    } */

    /**
     * 白名单校验：对流式
     */
    private boolean isWhiteList(String method, String uri) {
        boolean all = authProperties.getWhiteListAll().stream()
                .anyMatch(pattern -> MATCHER.match(pattern, uri)); // 短路
        boolean get = "GET".equalsIgnoreCase(method)
                && authProperties.getWhiteListGet().stream()
                .anyMatch(pattern -> MATCHER.match(pattern, uri));
        return all || get;
    }

    /*private boolean isWhiteList(String method, String uri) {
        boolean all = false;
        for (String pattern : authProperties.getWhiteListAll()) {
            if (MATCHER.match(pattern, uri)) {
                all = true;
                break;
            }
        }

        boolean get = false;
        if ("GET".equalsIgnoreCase(method)) {
            for (String pattern : authProperties.getWhiteListGet()) {
                if (MATCHER.match(pattern, uri)) {
                    get = true;
                    break;
                }
            }
        }

        return all || get;
    }*/
}
