// 题库本地缓存：localStorage 持久化 + TTL，减少重复请求、页面秒开
const PREFIX = 'quizcard:cache:'

interface Entry<T> {
  v: T
  t: number // 写入时间戳
}

/** 读取缓存，过期或不存在返回 null */
export function cacheGet<T>(key: string, ttlMs: number): T | null {
  try {
    const raw = localStorage.getItem(PREFIX + key)
    if (!raw) return null
    const entry = JSON.parse(raw) as Entry<T>
    if (Date.now() - entry.t > ttlMs) return null
    return entry.v
  } catch {
    return null
  }
}

export function cacheSet<T>(key: string, v: T): void {
  try {
    const entry: Entry<T> = { v, t: Date.now() }
    localStorage.setItem(PREFIX + key, JSON.stringify(entry))
  } catch {
    // 存储满等异常静默
  }
}

export function cacheDel(...keys: string[]): void {
  for (const k of keys) localStorage.removeItem(PREFIX + k)
}

/** 缓存键约定 */
export const CACHE_KEYS = {
  categories: 'categories', // 分类列表
  countByCategory: 'countByCategory' // 各分类题目数
} as const

/** 题库数据变更（增删改/导入/恢复）后调用，清除相关缓存 */
export function invalidateQuestionCache(): void {
  cacheDel(CACHE_KEYS.categories, CACHE_KEYS.countByCategory)
}
