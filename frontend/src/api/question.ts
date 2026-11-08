import http from './http'
import type { CategoryCount, PageResult, Question, QuestionDTO, QuestionQueryDTO } from '@/types'
import { cacheGet, cacheSet, CACHE_KEYS } from '@/composables/useLocalCache'

export const pageQuestions = (query: QuestionQueryDTO): Promise<PageResult<Question>> =>
  http.get('/question', { params: query })

export const addQuestion = (dto: QuestionDTO): Promise<Question> =>
  http.post('/question', dto)

export const batchImportQuestions = (dtos: QuestionDTO[]): Promise<Question[]> =>
  http.post('/question/batch', dtos)

export const updateQuestion = (dto: QuestionDTO): Promise<Question> =>
  http.put('/question', dto)

export const deleteQuestions = (ids: number[]): Promise<number> =>
  http.delete('/question', { data: ids })

export type PickMode = 'random' | 'sequential'

export interface PickQuestionParams {
  /** 分类ID，null/undefined = 全部 */
  categoryId?: number | null
  /** 题型，空 = 全部 */
  type?: string | null
  /** 需要排除的题目 ID（不重复抽卡 / 只抽未抽过） */
  excludeIds?: number[]
  /** 抽卡模式：随机或按 ID 顺序 */
  mode?: PickMode
  /** 顺序模式下从该 ID 之后继续 */
  afterId?: number | null
}

export const pickQuestion = (params: PickQuestionParams): Promise<Question | null> =>
  http.post('/question/random', {
    categoryId: params.categoryId ?? null,
    type: params.type || null,
    excludeIds: params.excludeIds ?? [],
    mode: params.mode ?? 'random',
    afterId: params.afterId ?? null
  })

export const countByCategory = (): Promise<CategoryCount[]> =>
  http.get('/question/count-by-category')

export const findByIds = (ids: number[]): Promise<Question[]> =>
  http.get('/question/by-ids', { params: { ids: ids.join(',') } })

/** 各分类题目数（带本地缓存 + 后台刷新 SWR），统计/抽卡页秒开 */
export const countByCategoryCached = (): Promise<CategoryCount[]> => {
  const hit = cacheGet<CategoryCount[]>(CACHE_KEYS.countByCategory, 60_000)
  if (hit) {
    countByCategory().then((list) => cacheSet(CACHE_KEYS.countByCategory, list)).catch(() => {})
    return Promise.resolve(hit)
  }
  return countByCategory().then((list) => {
    cacheSet(CACHE_KEYS.countByCategory, list)
    return list
  })
}
