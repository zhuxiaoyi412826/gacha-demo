package com.example.quizcard.service;

import com.example.quizcard.dto.CategoryDTO;
import com.example.quizcard.entity.QuestionCategory;

import java.util.List;

/**
 * 分类服务
 */
public interface CategoryService {

    /** 列出所有分类（按 createTime 升序） */
    List<QuestionCategory> list();

    /** 新增分类 */
    QuestionCategory add(CategoryDTO dto);

    /** 编辑分类名称 */
    QuestionCategory update(Long id, CategoryDTO dto);

    /** 删除分类（有题目则抛 BizException） */
    void delete(Long id);
}
