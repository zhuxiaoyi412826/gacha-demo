import { ref } from 'vue'
import { load, save, STORAGE_KEYS } from './useLocalStorage'

// 单例：模块加载即读取本地记忆并应用
const isDark = ref<boolean>(load(STORAGE_KEYS.darkMode, false))

function apply() {
  document.documentElement.classList.toggle('dark', isDark.value)
}
apply()

export function useDarkMode() {
  const toggleDark = () => {
    isDark.value = !isDark.value
    save(STORAGE_KEYS.darkMode, isDark.value)
    apply()
  }
  return { isDark, toggleDark }
}
