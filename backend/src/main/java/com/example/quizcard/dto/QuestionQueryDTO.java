package com.example.quizcard.dto;

import lombok.Data;

/**
 * 题目分页查询入参
 */
@Data
public class QuestionQueryDTO {

    /** 当前页，默认 1 */
    private Integer page = 1;

    /** 每页大小，默认 10 */
    private Integer size = 10;

    /** 分类ID，可空（不传 = 全部分类） */
    private Long categoryId;

    /** 关键词，按题干模糊匹配，可空 */
    private String keyword;

    /** 题目类型筛选：单选 / 多选 / 简答，可空（不传 = 全部类型） */
    private String type;
}
