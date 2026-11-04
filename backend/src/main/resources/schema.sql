-- 题目抽卡项目数据库建表脚本
-- 库由 JDBC URL 中 createDatabaseIfNotExist=true 自动创建（quiz_card）
-- 表使用 IF NOT EXISTS，启动多次执行安全

CREATE TABLE IF NOT EXISTS question_category
(
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    category_name VARCHAR(100) NOT NULL COMMENT '分类名称',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_name (category_name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='题目分类';

-- 默认分类（id=1）：题目不传 categoryId 时自动归到此分类
-- INSERT IGNORE 多次执行幂等；如果 id=1 已存在则不会覆盖现有名称
INSERT IGNORE INTO question_category (id, category_name) VALUES (1, '默认分类');

CREATE TABLE IF NOT EXISTS question
(
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    category_id BIGINT      NOT NULL DEFAULT 1 COMMENT '关联分类ID，默认 1=默认分类',
    title       TEXT        NOT NULL COMMENT '题干',
    type        VARCHAR(20) NOT NULL COMMENT '题目类型：单选/多选/简答',
    options     TEXT        COMMENT '选项 JSON 存储',
    answer      TEXT        NOT NULL COMMENT '参考答案',
    analysis    TEXT        COMMENT '解析，可为 null',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_category_id (category_id),
    KEY idx_type (type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='题目表';

-- 兼容已有数据库：把已为 NULL 的 category_id 回填为 1（默认分类）
UPDATE question SET category_id = 1 WHERE category_id IS NULL;
-- 将 category_id 列改回 NOT NULL DEFAULT 1
ALTER TABLE question MODIFY COLUMN category_id BIGINT NOT NULL DEFAULT 1 COMMENT '关联分类ID，默认 1=默认分类';
