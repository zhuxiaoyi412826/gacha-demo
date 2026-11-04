package com.example.quizcard.controller;

import com.example.quizcard.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 根路径健康检查 + 服务信息
 * 让访问 http://localhost:8088/ 时返回服务说明，而不是 NoResourceFound 500
 */
@RestController
public class RootController {

    @Operation(summary = "服务健康检查与服务信息")
    @GetMapping("/")
    public ApiResponse<Map<String, Object>> index() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("service", "quiz-card");
        info.put("version", "1.0.0");
        info.put("status", "running");
        info.put("apiBase", "/api");
        info.put("docs", "/doc.html");
        info.put("endpoints", Map.of(
                "categories", "GET /api/category",
                "questionsPage", "GET /api/question",
                "randomQuestion", "GET /api/question/random?categoryId=X",
                "chatStream", "GET /api/chat/stream?message=X"
        ));
        info.put("frontend", "http://localhost:5173");
        return ApiResponse.ok(info);
    }
}
