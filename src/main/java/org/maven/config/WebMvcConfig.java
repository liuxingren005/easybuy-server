package org.maven.config;

import lombok.RequiredArgsConstructor;
import org.maven.interceptor.RoleInterceptor;
import org.maven.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC ：注册登录/权限拦截器与跨域策略
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;
    private final RoleInterceptor roleInterceptor;

    /**
     * 顺序
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .order(1);
        registry.addInterceptor(roleInterceptor)
                .addPathPatterns("/**")
                .order(2);
    }

    /**
     * 跨域：协议 + 域名 + 端口
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 所有接口
                .allowedOriginPatterns("*") // 所有来源
                .allowedMethods("GET", "POST",
                        "PUT", "DELETE", "OPTIONS") // HTTP方法
                .allowedHeaders("*") // 所有请求头
                .allowCredentials(true) // Cookie
                .maxAge(3600); // 预检缓存时长
    }
}
