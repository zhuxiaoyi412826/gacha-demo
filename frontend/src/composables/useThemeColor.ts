import { watch } from 'vue'
import { useSettings } from './useSettings'

/** 可选主题主色（Element Plus 主色 presets） */
export const THEME_COLORS = [
  { name: '默认蓝', value: '#409eff' },
  { name: '清新绿', value: '#67c23a' },
  { name: '雅致紫', value: '#7c3aed' },
  { name: '暖橙', value: '#e6a23c' },
  { name: '胭脂红', value: '#f56c6c' }
]

/** 把两个 hex 颜色按权重混合，用于生成 Element Plus 的浅/深色阶 */
function mixColor(hex: string, target: string, weight: number): string {
  const parse = (s: string) => [
    parseInt(s.slice(1, 3), 16),
    parseInt(s.slice(3, 5), 16),
    parseInt(s.slice(5, 7), 16)
  ]
  const a = parse(hex)
  const b = parse(target)
  const out = a.map((v, i) => Math.round(v * (1 - weight) + b[i] * weight))
  return '#' + out.map((v) => v.toString(16).padStart(2, '0')).join('')
}

function applyThemeColor(color: string) {
  if (!/^#[0-9a-fA-F]{6}$/.test(color)) return
  const root = document.documentElement
  root.style.setProperty('--el-color-primary', color)
  root.style.setProperty('--el-color-primary-light-3', mixColor(color, '#ffffff', 0.3))
  root.style.setProperty('--el-color-primary-light-5', mixColor(color, '#ffffff', 0.5))
  root.style.setProperty('--el-color-primary-light-7', mixColor(color, '#ffffff', 0.7))
  root.style.setProperty('--el-color-primary-light-8', mixColor(color, '#ffffff', 0.8))
  root.style.setProperty('--el-color-primary-light-9', mixColor(color, '#ffffff', 0.9))
  root.style.setProperty('--el-color-primary-dark-2', mixColor(color, '#000000', 0.2))
}

const { settings } = useSettings()
watch(() => settings.themeColor, applyThemeColor, { immediate: true })
