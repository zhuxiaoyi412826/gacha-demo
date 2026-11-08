// 通用 localStorage 封装
const PREFIX = 'quizcard:'

export function load<T>(key: string, def: T): T {
  const raw = localStorage.getItem(PREFIX + key)
  if (raw == null) return def
  try {
    return JSON.parse(raw) as T
  } catch {
    return def
  }
}

export function save<T>(key: string, value: T): void {
  localStorage.setItem(PREFIX + key, JSON.stringify(value))
}

export function remove(key: string): void {
  localStorage.removeItem(PREFIX + key)
}

// 业务 keys
export const STORAGE_KEYS = {
  settings: 'settings', // 应用设置对象（见 useSettings）
  favorites: 'favorites', // number[]
  wrongs: 'wrongs', // number[]
  drawnIds: 'drawnIds', // 本轮已抽题目 ID（按分类区分）
  drawCount: 'drawCount', // 总抽卡次数
  drawLog: 'drawLog', // 每次抽卡的时间戳 number[]（趋势图用）
  drawHistory: 'drawHistory', // 累计抽过的题目 ID（去重，跨轮保留）
  darkMode: 'darkMode', // 旧版暗黑模式开关（仅用于迁移到 settings.themeMode）
  noRepeatDraw: 'noRepeatDraw' // 旧版不重复抽卡开关（仅用于迁移到 settings.noRepeatDraw）
} as const
