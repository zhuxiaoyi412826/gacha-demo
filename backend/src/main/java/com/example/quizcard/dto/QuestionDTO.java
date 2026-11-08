package com.example.quizcard.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 题目新增/编辑入参
 */
@Data
public class QuestionDTO {

    /** 编辑时传，新增时不传 */
    private Long id;

    /** 分类ID，可空；为 null 时按 categoryName 匹配，否则归到默认分类 */
    private Long categoryId;

    /** 分类名称（批量导入推荐使用）：按名称匹配已有分类，不存在则自动创建 */
    private String categoryName;

    @NotBlank(message = "题干不能为空")
    private String title;

    @NotBlank(message = "题目类型不能为空")
    private String type;

    /** 选项 JSON 字符串；简答题可不传 */
    private String options;

    @NotBlank(message = "参考答案不能为空")
    private String answer;

    /** 解析，可空 */
    private String analysis;
}
