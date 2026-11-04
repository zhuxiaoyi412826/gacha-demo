package com.example.quizcard.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringAI ChatClient Bean 配置
 * SpringAI starter 自动注入 ChatClient.Builder，这里 build() 一次得到全局单例 ChatClient
 */
@Configuration
public class AiClientConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
