import http from './http'
import type { Category } from '@/types'
import { cacheGet, cacheSet, CACHE_KEYS } from '@/composables/useLocalCache'

export const listCategories = (): Promise<Category[]> => http.get('/category')

export const addCategory = (categoryName: string): Promise<Category> =>
  http.post('/category', { categoryName })

export const updateCategory = (id: number, categoryName: string): Promise<Category> =>
  http.put(`/category/${id}`, { categoryName })

export const deleteCategory = (id: number): Promise<void> =>
  http.delete(`/category/${id}`)

/**
 * 分类列表（带本地缓存 + 后台刷新 SWR）：
 * 命中缓存立即返回，同时后台拉新数据更新缓存，页面秒开且数据不陈旧
 */
export const listCategoriesCached = (): Promise<Category[]> => {
  const hit = cacheGet<Category[]>(CACHE_KEYS.categories, 5 * 60_000)
  if (hit) {
    listCategories().then((list) => cacheSet(CACHE_KEYS.categories, list)).catch(() => {})
    return Promise.resolve(hit)
  }
  return listCategories().then((list) => {
    cacheSet(CACHE_KEYS.categories, list)
    return list
  })
}
