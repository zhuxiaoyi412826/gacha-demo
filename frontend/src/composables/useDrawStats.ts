import { ref, computed } from 'vue'
import { load, save, STORAGE_KEYS } from './useLocalStorage'

// 抽卡统计：收藏、错题、抽卡次数、本轮已抽题目
// 单例响应式状态

const favorites = ref<number[]>(load(STORAGE_KEYS.favorites, []))
const wrongs = ref<number[]>(load(STORAGE_KEYS.wrongs, []))
const drawCount = ref<number>(load(STORAGE_KEYS.drawCount, 0))
const drawLog = ref<number[]>(load(STORAGE_KEYS.drawLog, []))
const drawnIds = ref<number[]>(load(STORAGE_KEYS.drawnIds, []))
const drawHistory = ref<number[]>(load(STORAGE_KEYS.drawHistory, []))

function persistFavorites() { save(STORAGE_KEYS.favorites, favorites.value) }
function persistWrongs() { save(STORAGE_KEYS.wrongs, wrongs.value) }
function persistDrawCount() { save(STORAGE_KEYS.drawCount, drawCount.value) }
function persistDrawLog() { save(STORAGE_KEYS.drawLog, drawLog.value) }
function persistDrawnIds() { save(STORAGE_KEYS.drawnIds, drawnIds.value) }
function persistDrawHistory() { save(STORAGE_KEYS.drawHistory, drawHistory.value) }

/** 趋势图保留最近 20000 次抽卡时间戳，避免 localStorage 无限增长 */
const DRAW_LOG_LIMIT = 20000

/** 本地用户数据（收藏/错题/历史/抽卡记录），用于设置页导出与导入 */
export interface LocalUserData {
  favorites: number[]
  wrongs: number[]
  drawHistory: number[]
  drawCount: number
  drawLog: number[]
}

export function useDrawStats() {
  const isFavorite = (id: number) => favorites.value.includes(id)
  const isWrong = (id: number) => wrongs.value.includes(id)

  const toggleFavorite = (id: number) => {
    if (isFavorite(id)) {
      favorites.value = favorites.value.filter(x => x !== id)
    } else {
      favorites.value = [...favorites.value, id]
    }
    persistFavorites()
  }

  const toggleWrong = (id: number) => {
    if (isWrong(id)) {
      wrongs.value = wrongs.value.filter(x => x !== id)
    } else {
      wrongs.value = [...wrongs.value, id]
    }
    persistWrongs()
  }

  const recordDraw = (id: number) => {
    drawCount.value += 1
    persistDrawCount()
    // 趋势图数据：记录本次抽卡时间戳
    drawLog.value = [...drawLog.value, Date.now()].slice(-DRAW_LOG_LIMIT)
    persistDrawLog()
    if (id && !drawnIds.value.includes(id)) {
      drawnIds.value = [...drawnIds.value, id]
      persistDrawnIds()
    }
    // 累计历史：跨轮保留，去重
    if (id && !drawHistory.value.includes(id)) {
      drawHistory.value = [...drawHistory.value, id]
      persistDrawHistory()
    }
  }

  const resetDrawnIds = () => {
    drawnIds.value = []
    persistDrawnIds()
  }

  // ============ 设置页数据管理：清空 / 导出 / 导入 ============
  const clearFavorites = () => {
    favorites.value = []
    persistFavorites()
  }
  const clearWrongs = () => {
    wrongs.value = []
    persistWrongs()
  }
  const clearHistory = () => {
    drawHistory.value = []
    persistDrawHistory()
  }
  const clearDrawStats = () => {
    drawCount.value = 0
    drawLog.value = []
    persistDrawCount()
    persistDrawLog()
  }

  const exportLocalData = (): LocalUserData => ({
    favorites: [...favorites.value],
    wrongs: [...wrongs.value],
    drawHistory: [...drawHistory.value],
    drawCount: drawCount.value,
    drawLog: [...drawLog.value]
  })

  /** 导入本地数据：只覆盖传入的字段，未传的保持原样 */
  const importLocalData = (data: Partial<LocalUserData>) => {
    if (Array.isArray(data.favorites)) {
      favorites.value = data.favorites
      persistFavorites()
    }
    if (Array.isArray(data.wrongs)) {
      wrongs.value = data.wrongs
      persistWrongs()
    }
    if (Array.isArray(data.drawHistory)) {
      drawHistory.value = data.drawHistory
      persistDrawHistory()
    }
    if (typeof data.drawCount === 'number') {
      drawCount.value = data.drawCount
      persistDrawCount()
    }
    if (Array.isArray(data.drawLog)) {
      drawLog.value = data.drawLog
      persistDrawLog()
    }
  }

  const favoriteCount = computed(() => favorites.value.length)
  const wrongCount = computed(() => wrongs.value.length)
  const historyCount = computed(() => drawHistory.value.length)

  return {
    favorites,
    wrongs,
    drawCount,
    drawLog,
    drawnIds,
    drawHistory,
    favoriteCount,
    wrongCount,
    historyCount,
    isFavorite,
    isWrong,
    toggleFavorite,
    toggleWrong,
    recordDraw,
    resetDrawnIds,
    clearFavorites,
    clearWrongs,
    clearHistory,
    clearDrawStats,
    exportLocalData,
    importLocalData
  }
}
