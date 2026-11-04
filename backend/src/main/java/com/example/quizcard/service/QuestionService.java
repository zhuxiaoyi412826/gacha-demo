package com.example.quizcard.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.quizcard.dto.QuestionDTO;
import com.example.quizcard.dto.QuestionQueryDTO;
import com.example.quizcard.entity.Question;

import java.util.List;
import java.util.Map;

/**
 * 题目服务
 */
public interface QuestionService {

    /** 分页查询题目（含分类筛选 + 关键词搜索） */
    IPage<Question> page(QuestionQueryDTO query);

    /** 新增题目 */
    Question add(QuestionDTO dto);

    /** 批量新增题目（JSON 导入） */
    List<Question> batchAdd(List<QuestionDTO> dtos);

    /** 编辑题目 */
    Question update(QuestionDTO dto);

    /** 批量删除题目 */
    int deleteBatch(List<Long> ids);

    /** 按条件随机取一题：categoryId/type 任一可为空（null = 不限制该维度） */
    Question randomByCategory(Long categoryId, String type);

    /** 各分类题目总数：返回 [{categoryId, count}]，含 categoryId=null 的「未分类」分组 */
    List<Map<String, Object>> countByCategory();

    /** 按 id 列表批量查询题目（收藏/错题详情查看用） */
    List<Question> findByIds(List<Long> ids);
}
