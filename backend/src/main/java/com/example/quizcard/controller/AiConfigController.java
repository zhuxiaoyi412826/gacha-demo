package com.example.quizcard.controller;

import com.example.quizcard.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 配置接口：供设置页展示当前模型等只读信息
 */
@Tag(name = "AI 配置", description = "当前模型名等只读配置")
@RestController
@RequestMapping("/api/ai")
public class AiConfigController {

    @Value("${spring.ai.openai.chat.options.model:qwen-flash}")
    private String model;

    @Operation(summary = "AI 配置信息")
    @GetMapping("/config")
    public ApiResponse<Map<String, Object>> config() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("model", model);
        data.put("streamingSupported", true);
        return ApiResponse.ok(data);
    }
}
