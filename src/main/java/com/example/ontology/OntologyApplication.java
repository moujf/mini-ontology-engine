package com.example.ontology;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 应用入口。
 * 启动后内嵌 Tomcat 监听 8080 端口，静态页面由 Spring MVC 直接托管。
 */
@SpringBootApplication
public class OntologyApplication {
    public static void main(String[] args) {
        SpringApplication.run(OntologyApplication.class, args);
    }
}
