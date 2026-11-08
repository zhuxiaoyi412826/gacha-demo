package com.example.quizcard.controller;

import com.example.quizcard.common.ApiResponse;
import com.example.quizcard.dto.ChatRequestDTO;
import com.example.quizcard.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * AI 聊天接口（SSE 流式 + 非流式）
 */
@Tag(name = "AI 聊天", description = "SpringAI + 阿里百炼 qwen-flash，支持上下文与流式/非流式输出")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "SSE 流式聊天（可携带历史上下文）")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestBody ChatRequestDTO req) {
        return chatService.streamChat(req);
    }

    @Operation(summary = "非流式聊天：一次性返回完整回答")
    @PostMapping
    public ApiResponse<String> chat(@RequestBody ChatRequestDTO req) {
        return ApiResponse.ok(chatService.chat(req));
    }
}
