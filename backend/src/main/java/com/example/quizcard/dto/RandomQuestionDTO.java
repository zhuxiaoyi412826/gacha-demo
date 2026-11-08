package com.example.quizcard.dto;

import lombok.Data;

import java.util.List;

/**
 * 随机抽题入参
 */
@Data
public class RandomQuestionDTO {

    /** 分类ID，为 null 表示不限分类 */
    private Long categoryId;

    /** 题型，为空表示不限题型 */
    private String type;

    /** 需要排除的题目ID：不重复抽卡时传入本轮已抽过的题 */
    private List<Long> excludeIds;

    /** 抽卡模式：random（默认，随机抽取）/ sequential（按 ID 升序顺序取题） */
    private String mode;

    /** 顺序模式：从该题目 ID 之后继续取下一题（null 表示从最小 ID 开始） */
    private Long afterId;
}
