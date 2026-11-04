package com.example.quizcard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.quizcard.common.BizException;
import com.example.quizcard.dto.QuestionDTO;
import com.example.quizcard.dto.QuestionQueryDTO;
import com.example.quizcard.entity.Question;
import com.example.quizcard.mapper.QuestionMapper;
import com.example.quizcard.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 题目服务实现
 */
@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    /** 默认分类 ID：categoryId 为空时归到此分类（schema.sql 保证 id=1 存在） */
    private static final Long DEFAULT_CATEGORY_ID = 1L;

    private final QuestionMapper questionMapper;

    @Override
    public IPage<Question> page(QuestionQueryDTO query) {
        Page<Question> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .eq(query.getCategoryId() != null, Question::getCategoryId, query.getCategoryId())
                .eq(StringUtils.hasText(query.getType()), Question::getType, query.getType())
                .like(StringUtils.hasText(query.getKeyword()), Question::getTitle, query.getKeyword())
                .orderByDesc(Question::getCreateTime);
        return questionMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional
    public Question add(QuestionDTO dto) {
        if (dto.getCategoryId() == null) {
            dto.setCategoryId(DEFAULT_CATEGORY_ID);
        }
        Question question = new Question();
        applyDto(question, dto);
        question.setCreateTime(LocalDateTime.now());
        questionMapper.insert(question);
        return question;
    }

    @Override
    @Transactional
    public List<Question> batchAdd(List<QuestionDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            throw new BizException("导入列表不能为空");
        }
        List<Question> result = new ArrayList<>(dtos.size());
        LocalDateTime now = LocalDateTime.now();
        for (QuestionDTO dto : dtos) {
            // categoryId 为空时自动归到默认分类(id=1)
            if (dto.getCategoryId() == null) {
                dto.setCategoryId(DEFAULT_CATEGORY_ID);
            }
            if (!StringUtils.hasText(dto.getTitle())) {
                throw new BizException("存在题目的题干为空");
            }
            if (!StringUtils.hasText(dto.getType())) {
                throw new BizException("存在题目的类型为空");
            }
            if (!StringUtils.hasText(dto.getAnswer())) {
                throw new BizException("存在题目的参考答案为空");
            }
            // options 直接按字符串存储；前端模板示例即为 JSON 字符串
            Question question = new Question();
            applyDto(question, dto);
            question.setCreateTime(now);
            questionMapper.insert(question);
            result.add(question);
        }
        return result;
    }

    @Override
    @Transactional
    public Question update(QuestionDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("题目ID不能为空");
        }
        if (dto.getCategoryId() == null) {
            dto.setCategoryId(DEFAULT_CATEGORY_ID);
        }
        Question question = questionMapper.selectById(dto.getId());
        if (question == null) {
            throw new BizException(404, "题目不存在");
        }
        applyDto(question, dto);
        questionMapper.updateById(question);
        return question;
    }

    @Override
    @Transactional
    public int deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException("ids 不能为空");
        }
        return questionMapper.deleteBatchIds(ids);
    }

    @Override
    public Question randomByCategory(Long categoryId, String type) {
        // categoryId 与 type 任一为 null 都表示不限制该维度
        // 两个都为 null 时即从全部题目中随机
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .eq(categoryId != null, Question::getCategoryId, categoryId)
                .eq(StringUtils.hasText(type), Question::getType, type)
                .last("ORDER BY RAND() LIMIT 1");
        return questionMapper.selectOne(wrapper); // 可能为 null
    }

    @Override
    public List<Map<String, Object>> countByCategory() {
        // 简单实现：遍历 + count。题量不大场景下足够
        // 把 categoryId=null 的题归入「未分类」分组（key 用 null 保留）
        List<Question> all = questionMapper.selectList(null);
        Map<Long, Long> counter = new HashMap<>();
        for (Question q : all) {
            counter.merge(q.getCategoryId(), 1L, Long::sum);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, Long> e : counter.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("categoryId", e.getKey());
            item.put("count", e.getValue());
            result.add(item);
        }
        return result;
    }

    private void applyDto(Question entity, QuestionDTO dto) {
        entity.setCategoryId(dto.getCategoryId());
        entity.setTitle(dto.getTitle());
        entity.setType(dto.getType());
        entity.setOptions(dto.getOptions());
        entity.setAnswer(dto.getAnswer());
        entity.setAnalysis(dto.getAnalysis());
    }

    @Override
    public List<Question> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .in(Question::getId, ids)
                .orderByDesc(Question::getCreateTime));
    }
}
