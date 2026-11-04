package com.example.quizcard.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / Springdoc OpenAPI 元信息配置
 * 访问地址：http://localhost:8080/doc.html
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI quizCardOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("题目抽卡项目 API 文档")
                        .description("前后端分离题目抽卡项目，提供分类、题目、抽卡、AI 聊天接口")
                        .version("1.0.0")
                        .contact(new Contact().name("quiz-card")));
    }
}
