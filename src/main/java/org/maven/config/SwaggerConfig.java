package org.maven.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("新闻管理系统API文档")
                        .description("基于Spring Boot + MyBatis + Redis的新闻管理系统接口文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("news-pagehelper")
                                .email("admin@example.com")
                                .url("https://github.com/news-pagehelper"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
