package com.example.quizcard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目
 */
@Data
@TableName("question")
public class Question {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联分类ID */
    private Long categoryId;

    /** 题干 */
    private String title;

    /** 题目类型：单选 / 多选 / 简答 */
    private String type;

    /** 选项 JSON 字符串存储 */
    private String options;

    /** 参考答案 */
    private String answer;

    /** 解析，可为 null */
    private String analysis;

    /** 创建时间 */
    private LocalDateTime createTime;
}
