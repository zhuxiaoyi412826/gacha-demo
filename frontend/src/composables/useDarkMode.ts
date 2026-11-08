import { computed, ref, watch } from 'vue'
import { useSettings } from './useSettings'

// 暗黑模式：支持「浅色 / 深色 / 跟随系统」三态，由设置页控制
const { settings } = useSettings()
const media = window.matchMedia('(prefers-color-scheme: dark)')
const systemDark = ref(media.matches)
media.addEventListener('change', (e) => {
  systemDark.value = e.matches
})

const isDark = computed(() =>
  settings.themeMode === 'system' ? systemDark.value : settings.themeMode === 'dark'
)

watch(
  isDark,
  (v) => document.documentElement.classList.toggle('dark', v),
  { immediate: true }
)

export function useDarkMode() {
  /** 顶栏快捷切换：跟随系统时按当前实际效果切到相反模式 */
  const toggleDark = () => {
    settings.themeMode = isDark.value ? 'light' : 'dark'
  }
  return { isDark, toggleDark }
}
