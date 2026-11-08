package com.example.quizcard.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局跨域配置：本项目无登录鉴权，允许任意来源访问
 * 注意：浏览器通过局域网 IP（如 http://192.168.45.171:5173）打开页面时，
 * POST/PUT/DELETE 会携带该 IP 的 Origin，若只放行 localhost 会返回 403 Invalid CORS request
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
