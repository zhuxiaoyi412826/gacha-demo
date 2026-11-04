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

export const randomQuestion = (
  categoryId?: number | null,
  type?: string | null
): Promise<Question | null> => {
  const params: Record<string, unknown> = {}
  if (categoryId != null) params.categoryId = categoryId
  if (type) params.type = type
  return http.get('/question/random', { params })
}

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
