# 题目抽卡（Quiz Card）

一个轻量化的前后端分离刷题/抽卡应用：打开即用，无需登录注册。支持题库管理、随机抽卡、3D 翻卡看答案、收藏/错题标记、批量导入、简单统计与 AI 聊天答疑。

## 技术栈

| 端 | 技术 |
| --- | --- |
| 后端 | Spring Boot 3.5.13、Spring AI 1.0（DashScope OpenAI 兼容模式）、MyBatis-Plus 3.5.12（分页插件 + jsqlparser）、Knife4j（com.baizhukui 5.7.4）、Lombok、MySQL 8 |
| 前端 | Vue 3、Vite、TypeScript、Element Plus、Vue Router、Axios |
| 数据流 | 收藏 / 错题 / 抽卡次数存浏览器 localStorage（前缀 `quizcard:`），不入库 |

## 项目结构

```
├── backend/                  # Spring Boot 后端（端口 8088）
│   └── src/main/java/com/example/quizcard/
│       ├── controller/       # QuestionController / CategoryController / ChatController / RootController
│       ├── service/          # 业务层（含 impl）
│       ├── mapper/           # MyBatis-Plus Mapper
│       ├── entity/           # Question / QuestionCategory
│       ├── dto/              # QuestionDTO / QuestionQueryDTO / CategoryDTO
│       ├── config/           # MybatisPlusConfig（分页）/ Knife4jConfig / CorsConfig / AiClientConfig
│       └── common/           # ApiResponse / BizException / GlobalExceptionHandler
│   └── src/main/resources/
│       ├── application.yml   # 端口、数据源、Spring AI 配置
│       └── schema.sql        # 启动自动建表 + 默认分类初始化（幂等）
└── frontend/                 # Vue3 前端（端口 5173，/api 代理到 8088）
    └── src/
        ├── views/            # DrawCardView / QuestionBankView / CategoryManageView / StatisticsView / AiChatView
        ├── components/       # FlipCard（3D 翻卡）/ QuestionForm / AppLayout
        ├── composables/      # useLocalStorage / useDrawStats（收藏、错题、抽卡计数）
        ├── api/              # axios 封装 + 各模块接口
        └── router/           # 5 个页面路由
```

## 功能清单

| 页面 | 路由 | 功能 |
| --- | --- | --- |
| 抽卡 | `/draw` | 分类下拉（含「全部题目」）+ 题型下拉（单选/多选/简答）双维度筛选；随机抽题；3D 翻卡看答案；上一题/下一题历史；收藏、错题标记；本轮已抽计数；筛选条件记忆 |
| 题库管理 | `/bank` | 分页表格；分类/题型/关键词三重筛选；新增、编辑、单删、批量删除；批量导入 JSON（拖拽上传、解析预览、模板下载） |
| 分类管理 | `/category` | 分类 CRUD；存在题目的分类禁止删除（防脏数据） |
| 统计 | `/stats` | 抽卡总次数、收藏数、错题数、题目总数；各分类题目数进度条；点击收藏/错题卡片弹窗查看题目详情，可一键移除 |
| AI 聊天 | `/chat` | SSE 流式对话（对接阿里百炼 DashScope），气泡式聊天 UI |

### 业务规则

- 启动时自动建表，并 `INSERT IGNORE` 一条 **默认分类（id=1，名称「默认分类」）**
- 新增/导入题目不传 `categoryId` 时，自动归入默认分类（id=1）
- 随机抽题：categoryId 与 type 均可选，任一为空即不限制该维度，均空则从全部题目随机
- 收藏、错题、抽卡次数、当前筛选均存 localStorage，刷新不丢失

## 数据库设计（2 张表）

**question_category 题目分类**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键自增（id=1 为默认分类） |
| category_name | varchar(100) | 分类名称，唯一 |
| create_time | datetime | 创建时间 |

**question 题目**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键自增 |
| category_id | bigint | 关联分类，NOT NULL DEFAULT 1 |
| title | text | 题干 |
| type | varchar(20) | 单选 / 多选 / 简答 |
| options | text | 选项，JSON 字符串（简答可为空） |
| answer | text | 参考答案 |
| analysis | text | 解析，可空 |
| create_time | datetime | 创建时间 |

## 接口清单

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/api/category` | GET | 分类列表 |
| `/api/category` | POST | 新增分类 |
| `/api/category` | PUT | 修改分类名称 |
| `/api/category/{id}` | DELETE | 删除分类（有题目则拒绝） |
| `/api/question` | GET | 题目分页（page/size/categoryId/type/keyword） |
| `/api/question` | POST | 新增题目 |
| `/api/question` | PUT | 编辑题目 |
| `/api/question` | DELETE | 批量删除（ids 数组） |
| `/api/question/batch` | POST | 批量导入（JSON 数组，事务原子性） |
| `/api/question/random` | GET | 随机抽题（categoryId、type 均可选） |
| `/api/question/by-ids` | GET | 按 id 列表批量查询（收藏/错题详情） |
| `/api/question/count-by-category` | GET | 各分类题目数 |
| `/api/chat/stream?message=` | GET | AI 流式对话（SSE） |
| `/` | GET | 服务健康检查与端点索引 |

接口文档（Knife4j）：`http://localhost:8088/doc.html`

## 快速开始

### 1. 环境变量

```powershell
# 必填
$env:MYSQL_USER = "root"
$env:MYSQL_PASSWORD = "你的密码"
# AI 聊天功能需要（不配则聊天不可用，其余功能正常）
$env:ALI_BABA_API_KEY = "sk-xxx"
# 可选，默认 qwen-flash，可换 qwen-plus / qwen-max
$env:AI_MODEL_NAME = "qwen-flash"
```

### 2. 启动后端（8088）

```bash
cd backend
mvn spring-boot:run
```

首次启动自动执行 `schema.sql` 建表并初始化默认分类。

### 3. 启动前端（5173）

```bash
cd frontend
npm install
npm run dev
```

访问 `http://localhost:5173/draw` 开始抽卡。

## 批量导入模板格式

```json
[
  {
    "categoryId": 1,
    "title": "题干",
    "type": "单选",
    "options": "[{\"label\":\"A\",\"text\":\"选项一\"}]",
    "answer": "A",
    "analysis": "解析（可选）"
  },
  {
    "title": "不指定分类的题（自动归入默认分类 id=1）",
    "type": "简答",
    "answer": "参考答案"
  }
]
```

字段说明：`title` / `type` / `answer` 必填；`categoryId` / `options` / `analysis` 可选。模板可在题库管理页「批量导入」弹窗中直接下载。
