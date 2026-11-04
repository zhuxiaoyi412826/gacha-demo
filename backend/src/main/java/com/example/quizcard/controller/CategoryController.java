package com.example.quizcard.controller;

import com.example.quizcard.common.ApiResponse;
import com.example.quizcard.dto.CategoryDTO;
import com.example.quizcard.entity.QuestionCategory;
import com.example.quizcard.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 题目分类接口
 */
@Tag(name = "分类管理", description = "题库分类 CRUD")
@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "获取所有分类")
    @GetMapping
    public ApiResponse<List<QuestionCategory>> list() {
        return ApiResponse.ok(categoryService.list());
    }

    @Operation(summary = "新增分类")
    @PostMapping
    public ApiResponse<QuestionCategory> add(@Valid @RequestBody CategoryDTO dto) {
        return ApiResponse.ok(categoryService.add(dto));
    }

    @Operation(summary = "编辑分类名称")
    @PutMapping("/{id}")
    public ApiResponse<QuestionCategory> update(@PathVariable Long id, @Valid @RequestBody CategoryDTO dto) {
        return ApiResponse.ok(categoryService.update(id, dto));
    }

    @Operation(summary = "删除分类（存在题目则禁止）")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.ok();
    }
}
