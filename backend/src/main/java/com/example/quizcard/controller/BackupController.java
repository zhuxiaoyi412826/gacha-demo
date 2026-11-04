package com.example.quizcard.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.quizcard.common.ApiResponse;
import com.example.quizcard.common.BizException;
import com.example.quizcard.entity.Question;
import com.example.quizcard.entity.QuestionCategory;
import com.example.quizcard.mapper.CategoryMapper;
import com.example.quizcard.mapper.QuestionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 题库全量备份 / 恢复
 * 与「批量导入」不同：这是整库快照，恢复时先清空再按快照重建（保留原始 id 与 createTime）
 */
@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
@Tag(name = "备份恢复", description = "题库整库快照导出与恢复")
public class BackupController {

    private final CategoryMapper categoryMapper;
    private final QuestionMapper questionMapper;

    @Operation(summary = "导出整库快照", description = "返回全部分类 + 全部题目的 JSON 快照")
    @GetMapping("/export")
    public ApiResponse<Map<String, Object>> exportBackup() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("version", 1);
        payload.put("exportedAt", LocalDateTime.now().toString());
        payload.put("categories", categoryMapper.selectList(
                new LambdaQueryWrapper<QuestionCategory>().orderByAsc(QuestionCategory::getId)));
        payload.put("questions", questionMapper.selectList(
                new LambdaQueryWrapper<Question>().orderByAsc(Question::getId)));
        return ApiResponse.ok(payload);
    }

    @Operation(summary = "恢复整库快照", description = "清空现有题库后按快照重建（危险操作，前端需二次确认）")
    @PostMapping("/restore")
    @Transactional
    public ApiResponse<Map<String, Integer>> restoreBackup(@RequestBody BackupPayload payload) {
        if (payload == null || payload.getCategories() == null || payload.getQuestions() == null) {
            throw new BizException("备份文件格式不正确：缺少 categories 或 questions 字段");
        }
        // 先清空子表（题目）再清空分类表
        questionMapper.delete(new LambdaQueryWrapper<>());
        categoryMapper.delete(new LambdaQueryWrapper<>());
        // 按快照重建：先分类后题目，保留原始 id 与 createTime
        for (QuestionCategory c : payload.getCategories()) {
            categoryMapper.insert(c);
        }
        for (Question q : payload.getQuestions()) {
            // 快照里 categoryId 为空时归到默认分类，保持与新增逻辑一致
            if (q.getCategoryId() == null) {
                q.setCategoryId(1L);
            }
            questionMapper.insert(q);
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("categories", payload.getCategories().size());
        result.put("questions", payload.getQuestions().size());
        return ApiResponse.ok(result);
    }

    /** 备份文件结构 */
    @Data
    public static class BackupPayload {
        private Integer version;
        private String exportedAt;
        private List<QuestionCategory> categories;
        private List<Question> questions;
    }
}
