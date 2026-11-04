package com.example.quizcard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.quizcard.common.BizException;
import com.example.quizcard.dto.CategoryDTO;
import com.example.quizcard.entity.Question;
import com.example.quizcard.entity.QuestionCategory;
import com.example.quizcard.mapper.CategoryMapper;
import com.example.quizcard.mapper.QuestionMapper;
import com.example.quizcard.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分类服务实现
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final QuestionMapper questionMapper;

    @Override
    public List<QuestionCategory> list() {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<QuestionCategory>()
                        .orderByAsc(QuestionCategory::getCreateTime));
    }

    @Override
    @Transactional
    public QuestionCategory add(CategoryDTO dto) {
        // 校验重名
        long exists = categoryMapper.selectCount(new LambdaQueryWrapper<QuestionCategory>()
                .eq(QuestionCategory::getCategoryName, dto.getCategoryName()));
        if (exists > 0) {
            throw new BizException("分类名称已存在");
        }
        QuestionCategory category = new QuestionCategory();
        category.setCategoryName(dto.getCategoryName());
        category.setCreateTime(LocalDateTime.now());
        categoryMapper.insert(category);
        return category;
    }

    @Override
    @Transactional
    public QuestionCategory update(Long id, CategoryDTO dto) {
        QuestionCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BizException(404, "分类不存在");
        }
        // 校验重名（排除自身）
        long exists = categoryMapper.selectCount(new LambdaQueryWrapper<QuestionCategory>()
                .eq(QuestionCategory::getCategoryName, dto.getCategoryName())
                .ne(QuestionCategory::getId, id));
        if (exists > 0) {
            throw new BizException("分类名称已存在");
        }
        category.setCategoryName(dto.getCategoryName());
        categoryMapper.updateById(category);
        return category;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        QuestionCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BizException(404, "分类不存在");
        }
        // 校验：存在题目的分类禁止删除
        long questionCount = questionMapper.selectCount(new LambdaQueryWrapper<Question>()
                .eq(Question::getCategoryId, id));
        if (questionCount > 0) {
            throw new BizException("该分类下存在 " + questionCount + " 道题目，禁止删除");
        }
        categoryMapper.deleteById(id);
    }
}
