import { reactive, watch } from 'vue'
import { load, save, STORAGE_KEYS } from './useLocalStorage'

/** 主题模式：浅色 / 深色 / 跟随系统 */
export type ThemeMode = 'light' | 'dark' | 'system'
/** 抽卡模式：随机 / 顺序 / 只抽未抽过 */
export type DrawMode = 'random' | 'sequential' | 'unseen'
/** 答案字号 */
export type AnswerFontSize = 'small' | 'normal' | 'large'
/** 答案行距 */
export type AnswerLineHeight = 'compact' | 'normal' | 'loose'
/** 默认题型 */
export type DefaultQuestionType = '' | '单选' | '多选' | '简答'

export interface AppSettings {
  // 抽卡
  noRepeatDraw: boolean
  drawMode: DrawMode
  defaultCategoryId: number | null
  defaultType: DefaultQuestionType
  autoFlip: boolean
  autoSpeak: boolean
  shortcutsEnabled: boolean
  // 外观与交互
  themeMode: ThemeMode
  themeColor: string
  animationsEnabled: boolean
  soundEnabled: boolean
  soundVolume: number // 0-100
  pageSoundEnabled: boolean // 上一题/下一题翻页音效
  // 答案排版
  answerFontSize: AnswerFontSize
  answerLineHeight: AnswerLineHeight
  markdownEnabled: boolean
  // AI
  streamingEnabled: boolean
  aiContextRounds: number // 0 = 不携带上下文
}

const DEFAULTS: AppSettings = {
  noRepeatDraw: false,
  drawMode: 'random',
  defaultCategoryId: null,
  defaultType: '',
  autoFlip: false,
  autoSpeak: false,
  shortcutsEnabled: true,
  themeMode: 'light',
  themeColor: '#409eff',
  animationsEnabled: true,
  soundEnabled: true,
  soundVolume: 60,
  pageSoundEnabled: true,
  answerFontSize: 'normal',
  answerLineHeight: 'normal',
  markdownEnabled: true,
  streamingEnabled: true,
  aiContextRounds: 3
}

// 旧版本把开关单独存在 darkMode / noRepeatDraw 两个 key，这里一次性迁移进 settings
const stored = load<Partial<AppSettings>>(STORAGE_KEYS.settings, {})
const legacyDarkMode = load<boolean>(STORAGE_KEYS.darkMode, false)
const legacyNoRepeatDraw = load<boolean>(STORAGE_KEYS.noRepeatDraw, false)

const settings = reactive<AppSettings>({
  ...DEFAULTS,
  ...stored,
  themeMode: stored.themeMode ?? (legacyDarkMode ? 'dark' : 'light'),
  noRepeatDraw: stored.noRepeatDraw ?? legacyNoRepeatDraw
})

// 任何设置项变化都自动落盘，设置页无需逐项保存
watch(settings, () => save(STORAGE_KEYS.settings, { ...settings }), { deep: true })

export function useSettings() {
  return { settings }
}

/** 字号档位 → 实际 px（抽卡卡片、详情弹窗共用） */
export const FONT_SIZE_MAP: Record<AnswerFontSize, string> = {
  small: '14px',
  normal: '16px',
  large: '18px'
}

/** 行距档位 → line-height 倍数 */
export const LINE_HEIGHT_MAP: Record<AnswerLineHeight, number> = {
  compact: 1.5,
  normal: 1.7,
  loose: 2.0
}
