package com.example.quizcard.service;

import com.example.quizcard.dto.ChatMessageDTO;
import com.example.quizcard.dto.ChatRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 聊天服务：调用 SpringAI ChatClient 对接阿里百炼 qwen-flash
 * 支持把前端裁剪后的上下文历史一并送入模型
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String SYSTEM_PROMPT = "你是一名面试题学习助手，回答准确、简洁，必要时分点说明。";

    private final ChatClient chatClient;

    /**
     * 流式聊天：返回 token 流，前端用 fetch 读取 SSE
     */
    public Flux<String> streamChat(ChatRequestDTO req) {
        return chatClient.prompt(buildPrompt(req)).stream().content();
    }

    /**
     * 非流式聊天：一次性返回完整回答（设置里关闭流式输出时使用）
     */
    public String chat(ChatRequestDTO req) {
        return chatClient.prompt(buildPrompt(req)).call().content();
    }

    /** 把「系统提示 + 历史上下文 + 本次提问」拼成 Prompt */
    private Prompt buildPrompt(ChatRequestDTO req) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM_PROMPT));
        if (req.getHistory() != null) {
            for (ChatMessageDTO h : req.getHistory()) {
                if (h == null || !StringUtils.hasText(h.getContent())) {
                    continue;
                }
                if ("assistant".equals(h.getRole())) {
                    messages.add(new AssistantMessage(h.getContent()));
                } else {
                    messages.add(new UserMessage(h.getContent()));
                }
            }
        }
        messages.add(new UserMessage(req.getMessage()));
        return new Prompt(messages);
    }
}
