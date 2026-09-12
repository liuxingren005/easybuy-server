package org.maven;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.maven.mapper")
public class EasybuyApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasybuyApplication.class, args);
    }
}
