package com.example.quizcard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.quizcard.common.BizException;
import com.example.quizcard.dto.QuestionDTO;
import com.example.quizcard.dto.QuestionQueryDTO;
import com.example.quizcard.dto.RandomQuestionDTO;
import com.example.quizcard.entity.Question;
import com.example.quizcard.entity.QuestionCategory;
import com.example.quizcard.mapper.CategoryMapper;
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
    private final CategoryMapper categoryMapper;

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
        // 同名分类只解析/创建一次，避免导入时重复查询和重复建分类
        Map<String, Long> resolvedCategories = new HashMap<>();
        for (QuestionDTO dto : dtos) {
            if (!StringUtils.hasText(dto.getTitle())) {
                throw new BizException("存在题目的题干为空");
            }
            if (!StringUtils.hasText(dto.getType())) {
                throw new BizException("存在题目的类型为空");
            }
            if (!StringUtils.hasText(dto.getAnswer())) {
                throw new BizException("存在题目的参考答案为空");
            }
            // 分类解析：categoryId 优先（必须存在），其次 categoryName，最后默认分类
            dto.setCategoryId(resolveCategoryId(dto, resolvedCategories));
            // options 直接按字符串存储；前端模板示例即为 JSON 字符串
            Question question = new Question();
            applyDto(question, dto);
            question.setCreateTime(now);
            questionMapper.insert(question);
            result.add(question);
        }
        return result;
    }

    /**
     * 解析导入题目所属分类：
     * 1. 传了 categoryId：必须存在于 question_category，否则报错（避免产生孤儿题目）
     * 2. 没传 categoryId 但传了 categoryName：按名称匹配，不存在则自动创建
     * 3. 都没传：归到默认分类(id=1)
     */
    private Long resolveCategoryId(QuestionDTO dto, Map<String, Long> resolvedCategories) {
        if (dto.getCategoryId() != null) {
            if (categoryMapper.selectById(dto.getCategoryId()) == null) {
                throw new BizException("分类 ID " + dto.getCategoryId()
                        + " 不存在（题目：" + dto.getTitle() + "），请检查导入文件的 categoryId，或改用 categoryName");
            }
            return dto.getCategoryId();
        }
        if (!StringUtils.hasText(dto.getCategoryName())) {
            return DEFAULT_CATEGORY_ID;
        }
        String name = dto.getCategoryName().trim();
        if (resolvedCategories.containsKey(name)) {
            return resolvedCategories.get(name);
        }
        QuestionCategory existing = categoryMapper.selectOne(
                new LambdaQueryWrapper<QuestionCategory>()
                        .eq(QuestionCategory::getCategoryName, name)
                        .last("LIMIT 1"));
        Long categoryId;
        if (existing != null) {
            categoryId = existing.getId();
        } else {
            QuestionCategory category = new QuestionCategory();
            category.setCategoryName(name);
            category.setCreateTime(LocalDateTime.now());
            categoryMapper.insert(category);
            categoryId = category.getId();
        }
        resolvedCategories.put(name, categoryId);
        return categoryId;
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
    public Question pick(RandomQuestionDTO req) {
        boolean sequential = "sequential".equals(req.getMode());
        Question q = selectPick(req, sequential, req.getAfterId());
        // 顺序模式走到末尾后，从头再找一道（已抽过的会被 excludeIds 过滤掉）
        if (q == null && sequential && req.getAfterId() != null) {
            q = selectPick(req, true, null);
        }
        return q; // 可能为 null（当前筛选下已没有可抽的题）
    }

    private Question selectPick(RandomQuestionDTO req, boolean sequential, Long afterId) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .eq(req.getCategoryId() != null, Question::getCategoryId, req.getCategoryId())
                .eq(StringUtils.hasText(req.getType()), Question::getType, req.getType())
                .notIn(req.getExcludeIds() != null && !req.getExcludeIds().isEmpty(),
                        Question::getId, req.getExcludeIds());
        if (sequential) {
            wrapper.gt(afterId != null, Question::getId, afterId)
                    .orderByAsc(Question::getId)
                    .last("LIMIT 1");
        } else {
            wrapper.last("ORDER BY RAND() LIMIT 1");
        }
        return questionMapper.selectOne(wrapper);
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
