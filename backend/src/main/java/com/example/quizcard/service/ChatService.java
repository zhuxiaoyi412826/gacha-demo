package com.example.quizcard.service;

import org.springframework.ai.chat.client.ChatClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * AI 聊天服务：调用 SpringAI ChatClient 对接阿里百炼 qwen-flash
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;

    /**
     * 流式聊天：返回 token 流，前端 EventSource 接收
     */
    public Flux<String> streamChat(String message) {
        return chatClient.prompt(message).stream().content();
    }
}
