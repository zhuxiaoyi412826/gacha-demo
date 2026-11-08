package com.example.quizcard.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.quizcard.common.ApiResponse;
import com.example.quizcard.dto.QuestionDTO;
import com.example.quizcard.dto.QuestionQueryDTO;
import com.example.quizcard.dto.RandomQuestionDTO;
import com.example.quizcard.entity.Question;
import com.example.quizcard.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 题目接口
 */
@Tag(name = "题目管理", description = "题库 CRUD + 随机抽卡 + 分类统计")
@RestController
@RequestMapping("/api/question")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @Operation(summary = "分页查询题目（支持分类筛选 + 关键词搜索）")
    @GetMapping
    public ApiResponse<IPage<Question>> page(QuestionQueryDTO query) {
        return ApiResponse.ok(questionService.page(query));
    }

    @Operation(summary = "新增题目")
    @PostMapping
    public ApiResponse<Question> add(@Valid @RequestBody QuestionDTO dto) {
        return ApiResponse.ok(questionService.add(dto));
    }

    @Operation(summary = "批量导入题目（JSON 数组）")
    @PostMapping("/batch")
    public ApiResponse<List<Question>> batchAdd(@RequestBody List<QuestionDTO> dtos) {
        return ApiResponse.ok(questionService.batchAdd(dtos));
    }

    @Operation(summary = "编辑题目")
    @PutMapping
    public ApiResponse<Question> update(@Valid @RequestBody QuestionDTO dto) {
        return ApiResponse.ok(questionService.update(dto));
    }

    @Operation(summary = "批量删除题目")
    @DeleteMapping
    public ApiResponse<Integer> deleteBatch(@RequestBody List<Long> ids) {
        return ApiResponse.ok(questionService.deleteBatch(ids));
    }

    @Operation(summary = "抽一道题（抽卡核心：categoryId/type/excludeIds/mode/afterId 均可选）")
    @PostMapping("/random")
    public ApiResponse<Question> random(@RequestBody(required = false) RandomQuestionDTO req) {
        RandomQuestionDTO dto = req == null ? new RandomQuestionDTO() : req;
        return ApiResponse.ok(questionService.pick(dto));
    }

    @Operation(summary = "各分类题目总数")
    @GetMapping("/count-by-category")
    public ApiResponse<List<Map<String, Object>>> countByCategory() {
        return ApiResponse.ok(questionService.countByCategory());
    }

    @Operation(summary = "按 id 列表批量查询题目（收藏/错题详情查看）")
    @GetMapping("/by-ids")
    public ApiResponse<List<Question>> findByIds(@RequestParam List<Long> ids) {
        return ApiResponse.ok(questionService.findByIds(ids));
    }
}
