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
    resetDrawnIds
  }
}
