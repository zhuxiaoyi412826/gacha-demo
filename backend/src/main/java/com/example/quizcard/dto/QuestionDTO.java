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

    /** 分类ID，可空（未分类题目）；为 null 表示不归类 */
    private Long categoryId;

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
