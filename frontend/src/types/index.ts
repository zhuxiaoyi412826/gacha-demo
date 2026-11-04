// 后端统一响应包装
export interface ApiResponse<T = unknown> {
  code: number
  msg: string
  data: T
}

// 分类实体
export interface Category {
  id: number
  categoryName: string
  createTime?: string
}

// 题目实体
export interface Question {
  id: number
  /** 分类ID，可空（未分类） */
  categoryId: number | null
  title: string
  type: QuestionType
  options?: string | null
  answer: string
  analysis?: string | null
  createTime?: string
}

// 题目类型字面量（含 '' / null 表示不限制）
export type QuestionType = '单选' | '多选' | '简答'

// 题目新增/编辑入参
export interface QuestionDTO {
  id?: number
  /** 分类ID，可空（未分类） */
  categoryId?: number | null
  title: string
  type: QuestionType
  options?: string | null
  answer: string
  analysis?: string | null
}

// 题目分页查询入参
export interface QuestionQueryDTO {
  page: number
  size: number
  categoryId?: number | null
  keyword?: string
  /** 题型筛选：单选/多选/简答；空表示不限 */
  type?: QuestionType | ''
}

// MyBatis-Plus 分页响应
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// 各分类题目数
export interface CategoryCount {
  categoryId: number
  count: number
}
