<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="head">
          <el-icon><Setting /></el-icon>
          <span class="head-title">设置</span>
          <el-input
            v-model="search"
            class="search"
            placeholder="搜索设置项，如：音效 / 主题 / 快捷键 / 清理"
            clearable
          />
          <span class="auto-save">修改后自动保存</span>
        </div>
      </template>

      <!-- 抽卡设置 -->
      <div v-show="matches('抽卡 不重复 模式 随机 顺序 未抽过 默认分类 默认题型 自动翻面 自动朗读 快捷键 本轮 重置')" class="section">
        <h4>抽卡设置</h4>
        <div class="row">
          <div class="text">
            <div class="label">不重复抽卡</div>
            <div class="desc">本轮内已抽过的题不再出现，直到点击「重置本轮」</div>
          </div>
          <el-switch v-model="settings.noRepeatDraw" />
        </div>
        <div class="row">
          <div class="text">
            <div class="label">抽卡模式</div>
            <div class="desc">顺序模式按题目 ID 从小到大依次出题；只抽未抽过会排除所有历史抽过的题</div>
          </div>
          <el-radio-group v-model="settings.drawMode">
            <el-radio-button value="random">随机</el-radio-button>
            <el-radio-button value="sequential">顺序</el-radio-button>
            <el-radio-button value="unseen">只抽未抽过</el-radio-button>
          </el-radio-group>
        </div>
        <div class="row">
          <div class="text">
            <div class="label">默认分类</div>
            <div class="desc">进入抽卡页时自动套用该分类，修改筛选只影响当前这一轮</div>
          </div>
          <el-select
            v-model="settings.defaultCategoryId"
            class="ctrl"
            clearable
            :value-on-clear="null"
            placeholder="全部题目"
          >
            <el-option v-for="c in categories" :key="c.id" :label="c.categoryName" :value="c.id" />
          </el-select>
        </div>
        <div class="row">
          <div class="text">
            <div class="label">默认题型</div>
            <div class="desc">进入抽卡页时自动套用该题型</div>
          </div>
          <el-select v-model="settings.defaultType" class="ctrl" clearable :value-on-clear="''" placeholder="全部题型">
            <el-option label="单选" value="单选" />
            <el-option label="多选" value="多选" />
            <el-option label="简答" value="简答" />
          </el-select>
        </div>
        <div class="row">
          <div class="text">
            <div class="label">抽到后自动翻面</div>
            <div class="desc">抽到新题 3 秒后自动翻到答案面</div>
          </div>
          <el-switch v-model="settings.autoFlip" />
        </div>
        <div class="row">
          <div class="text">
            <div class="label">抽到后自动朗读</div>
            <div class="desc">用浏览器语音合成（TTS）朗读题干</div>
          </div>
          <el-switch v-model="settings.autoSpeak" />
        </div>
        <div class="row">
          <div class="text">
            <div class="label">键盘快捷键</div>
            <div class="desc">空格 = 抽卡/翻面，←/→ = 上一题/下一题，1 = 收藏，2 = 错题</div>
          </div>
          <el-switch v-model="settings.shortcutsEnabled" />
        </div>
        <div class="row">
          <div class="text">
            <div class="label">本轮抽卡进度</div>
            <div class="desc">本轮已抽 {{ drawnIds.length }} 道题{{ settings.noRepeatDraw ? '，这些题目不会再出现' : '' }}</div>
          </div>
          <el-button :disabled="drawnIds.length === 0" @click="onResetRound">重置本轮</el-button>
        </div>
      </div>

      <el-divider v-show="matches('抽卡 不重复 模式 随机 顺序 未抽过 默认分类 默认题型 自动翻面 自动朗读 快捷键 本轮 重置')" />

      <!-- 外观与交互 -->
      <div v-show="matches('外观 主题 暗黑 深色 浅色 跟随系统 主题色 颜色 动画 音效 音量 翻页')" class="section">
        <h4>外观与交互</h4>
        <div class="row">
          <div class="text">
            <div class="label">主题模式</div>
            <div class="desc">跟随系统时会随操作系统的深浅色设置自动切换</div>
          </div>
          <el-radio-group v-model="settings.themeMode">
            <el-radio-button value="light">浅色</el-radio-button>
            <el-radio-button value="dark">深色</el-radio-button>
            <el-radio-button value="system">跟随系统</el-radio-button>
          </el-radio-group>
        </div>
        <div class="row">
          <div class="text">
            <div class="label">主题色</div>
            <div class="desc">Element Plus 主色，按钮、标签、链接会同步变化</div>
          </div>
          <div class="colors">
            <span
              v-for="c in THEME_COLORS"
              :key="c.value"
              class="color-item"
              :class="{ active: settings.themeColor === c.value }"
              :title="c.name"
              @click="settings.themeColor = c.value"
            >
              <span class="dot" :style="{ background: c.value }"></span>
              <span class="color-name">{{ c.name }}</span>
            </span>
          </div>
        </div>
        <div class="row">
          <div class="text">
            <div class="label">卡片动画</div>
            <div class="desc">关闭后去掉 3D 翻转过渡与入场动画，低配设备更流畅</div>
          </div>
          <el-switch v-model="settings.animationsEnabled" />
        </div>
        <div class="row">
          <div class="text">
            <div class="label">抽卡音效</div>
            <div class="desc">总开关：翻卡 whoosh、抽题 pop 与翻页提示音</div>
          </div>
          <el-switch v-model="settings.soundEnabled" />
        </div>
        <div class="row">
          <div class="text">
            <div class="label">音量</div>
            <div class="desc">范围 0-100，仅在音效开启时生效</div>
          </div>
          <div class="volume">
            <el-slider
              v-model="settings.soundVolume"
              :min="0"
              :max="100"
              :disabled="!settings.soundEnabled"
              style="width: 200px"
            />
            <span class="volume-value">{{ settings.soundVolume }}</span>
            <el-button size="small" :disabled="!settings.soundEnabled" @click="onPreviewSound">试听</el-button>
          </div>
        </div>
        <div class="row">
          <div class="text">
            <div class="label">翻页音效</div>
            <div class="desc">点击「上一题 / 下一题」切换题目时的短提示音</div>
          </div>
          <el-switch v-model="settings.pageSoundEnabled" :disabled="!settings.soundEnabled" />
        </div>
      </div>

      <el-divider v-show="matches('外观 主题 暗黑 深色 浅色 跟随系统 主题色 颜色 动画 音效 音量 翻页')" />

      <!-- 答案排版 -->
      <div v-show="matches('排版 字号 行距 Markdown 渲染 换行')" class="section">
        <h4>答案排版</h4>
        <div class="row">
          <div class="text">
            <div class="label">字号</div>
            <div class="desc">作用于抽卡卡片和详情弹窗里的答案/解析</div>
          </div>
          <el-radio-group v-model="settings.answerFontSize">
            <el-radio-button value="small">小</el-radio-button>
            <el-radio-button value="normal">中</el-radio-button>
            <el-radio-button value="large">大</el-radio-button>
          </el-radio-group>
        </div>
        <div class="row">
          <div class="text">
            <div class="label">行距</div>
            <div class="desc">长答案阅读时建议用「宽松」</div>
          </div>
          <el-radio-group v-model="settings.answerLineHeight">
            <el-radio-button value="compact">紧凑</el-radio-button>
            <el-radio-button value="normal">标准</el-radio-button>
            <el-radio-button value="loose">宽松</el-radio-button>
          </el-radio-group>
        </div>
        <div class="row">
          <div class="text">
            <div class="label">Markdown 渲染</div>
            <div class="desc">关闭后按纯文本显示（保留原始换行），适合手机竖屏看长答案</div>
          </div>
          <el-switch v-model="settings.markdownEnabled" />
        </div>
      </div>

      <el-divider v-show="matches('排版 字号 行距 Markdown 渲染 换行')" />

      <!-- AI 设置 -->
      <div v-show="matches('AI 模型 流式 上下文 轮数 聊天')" class="section">
        <h4>AI 设置</h4>
        <div class="row">
          <div class="text">
            <div class="label">当前模型</div>
            <div class="desc">由后端环境变量 AI_MODEL_NAME 决定，只读</div>
          </div>
          <el-tag type="info">{{ aiConfig?.model || '未获取到' }}</el-tag>
        </div>
        <div class="row">
          <div class="text">
            <div class="label">流式输出</div>
            <div class="desc">开启后回答逐字输出；关闭后一次性返回完整回答</div>
          </div>
          <el-switch v-model="settings.streamingEnabled" />
        </div>
        <div class="row">
          <div class="text">
            <div class="label">上下文轮数</div>
            <div class="desc">每次提问携带最近 N 轮对话（每轮一问一答），0 表示不携带上下文</div>
          </div>
          <el-input-number v-model="settings.aiContextRounds" :min="0" :max="10" />
        </div>
      </div>

      <el-divider v-show="matches('AI 模型 流式 上下文 轮数 聊天')" />

      <!-- 数据管理 -->
      <div v-show="matches('数据 导出 导入 备份 恢复 清空 清理 缓存 存储 用量 localStorage')" class="section">
        <h4>数据管理</h4>
        <div class="row">
          <div class="text">
            <div class="label">导出本地数据</div>
            <div class="desc">收藏、错题、历史、抽卡记录打包为 JSON，换设备/换浏览器可带走</div>
          </div>
          <el-button @click="onExportLocal">导出</el-button>
        </div>
        <div class="row">
          <div class="text">
            <div class="label">导入本地数据</div>
            <div class="desc">用导出的 JSON 覆盖当前本地记录</div>
          </div>
          <el-button @click="localImportRef?.click()">导入</el-button>
          <input
            ref="localImportRef"
            type="file"
            accept=".json,application/json"
            style="display: none"
            @change="onImportLocal"
          />
        </div>
        <div class="row">
          <div class="text">
            <div class="label">题库备份 / 恢复</div>
            <div class="desc">备份的是数据库里的题目与分类（不是本地记录）</div>
          </div>
          <div class="btn-group">
            <el-button @click="onExportBackup">导出整库</el-button>
            <el-button @click="backupRestoreRef?.click()">恢复整库</el-button>
          </div>
          <input
            ref="backupRestoreRef"
            type="file"
            accept=".json,application/json"
            style="display: none"
            @change="onRestoreBackupFile"
          />
        </div>
        <div class="row clear-row">
          <div class="text">
            <div class="label">清空本地数据</div>
            <div class="desc">勾选要清除的项目，操作不可恢复，会二次确认</div>
          </div>
          <div class="clear-actions">
            <el-checkbox v-model="clearTargets.cache">题库缓存</el-checkbox>
            <el-checkbox v-model="clearTargets.favorites">收藏</el-checkbox>
            <el-checkbox v-model="clearTargets.wrongs">错题</el-checkbox>
            <el-checkbox v-model="clearTargets.history">历史</el-checkbox>
            <el-checkbox v-model="clearTargets.stats">抽卡统计</el-checkbox>
            <el-button type="danger" plain size="small" @click="onClearLocal">清空所选</el-button>
          </div>
        </div>
        <div class="row storage-row">
          <div class="text">
            <div class="label">本地存储用量</div>
            <div class="desc">合计约 {{ formatSize(storageTotal) }}，抽卡日志上限 20000 条</div>
          </div>
          <el-button size="small" @click="refreshStorage">刷新</el-button>
        </div>
        <el-table :data="storageRows" size="small" max-height="220" style="width: 100%">
          <el-table-column prop="label" label="项目" min-width="160" />
          <el-table-column prop="count" label="条数" width="110" />
          <el-table-column label="大小" width="120">
            <template #default="{ row }">{{ formatSize(row.size) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Setting } from '@element-plus/icons-vue'
import { getAiConfig } from '@/api/ai'
import type { AiConfig } from '@/api/ai'
import { exportBackup, restoreBackup } from '@/api/backup'
import type { BackupPayload } from '@/api/backup'
import { listCategoriesCached } from '@/api/category'
import { invalidateQuestionCache } from '@/composables/useLocalCache'
import { useDrawStats } from '@/composables/useDrawStats'
import { useSettings } from '@/composables/useSettings'
import { THEME_COLORS } from '@/composables/useThemeColor'
import { playFlip } from '@/utils/sound'
import type { Category } from '@/types'

const { settings } = useSettings()
const {
  drawnIds,
  resetDrawnIds,
  clearFavorites,
  clearWrongs,
  clearHistory,
  clearDrawStats,
  exportLocalData,
  importLocalData
} = useDrawStats()

const categories = ref<Category[]>([])
const aiConfig = ref<AiConfig | null>(null)
const search = ref('')
const localImportRef = ref<HTMLInputElement>()
const backupRestoreRef = ref<HTMLInputElement>()

// ============ 设置项搜索 ============
const matches = (...keywords: string[]) => {
  const kw = search.value.trim().toLowerCase()
  if (!kw) return true
  return keywords.join(' ').toLowerCase().includes(kw)
}

// ============ 抽卡 ============
const onResetRound = () => {
  resetDrawnIds()
  ElMessage.success('已重置本轮抽卡记录')
}

// ============ 音效试听 ============
const onPreviewSound = () => playFlip()

// ============ 本地数据导出 / 导入 ============
const onExportLocal = () => {
  const payload = {
    version: 1,
    exportedAt: new Date().toISOString(),
    ...exportLocalData()
  }
  const text = JSON.stringify(payload, null, 2)
  const blob = new Blob([text], { type: 'application/json;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `quizcard-local-${new Date().toISOString().slice(0, 19).replace(/[T:]/g, '-')}.json`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  ElMessage.success('本地数据已导出')
}

const onImportLocal = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  try {
    const data = JSON.parse(await file.text()) as Partial<ReturnType<typeof exportLocalData>>
    await ElMessageBox.confirm(
      `导入将覆盖当前本地记录：收藏 ${data.favorites?.length ?? 0} 条、错题 ${data.wrongs?.length ?? 0} 条、历史 ${data.drawHistory?.length ?? 0} 条。确定继续？`,
      '导入本地数据',
      { type: 'warning', confirmButtonText: '确认导入', cancelButtonText: '取消' }
    )
    importLocalData(data)
    refreshStorage()
    ElMessage.success('本地数据导入完成')
  } catch (err) {
    if (err === 'cancel' || err === 'close') return
    ElMessage.error('导入失败：' + (err as Error).message)
  }
}

// ============ 题库备份 / 恢复 ============
const onExportBackup = async () => {
  const payload = await exportBackup()
  const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `quizcard-backup-${new Date().toISOString().slice(0, 19).replace(/[T:]/g, '-')}.json`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  ElMessage.success(`已导出备份：${payload.questions.length} 道题、${payload.categories.length} 个分类`)
}

const onRestoreBackupFile = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  let payload: BackupPayload
  try {
    payload = JSON.parse(await file.text()) as BackupPayload
    if (!Array.isArray(payload.categories) || !Array.isArray(payload.questions)) {
      throw new Error('缺少 categories/questions 数组')
    }
  } catch (err) {
    ElMessage.error('备份文件解析失败：' + (err as Error).message)
    return
  }
  await ElMessageBox.confirm(
    `恢复将清空现有题库，并以备份中的 ${payload.questions.length} 道题、${payload.categories.length} 个分类重建。确定继续？`,
    '危险操作',
    { type: 'error', confirmButtonText: '确认恢复', cancelButtonText: '取消' }
  )
  const res = await restoreBackup(payload)
  ElMessage.success(`恢复完成：${res.questions} 道题、${res.categories} 个分类`)
  invalidateQuestionCache()
  categories.value = await listCategoriesCached()
}

// ============ 清空本地数据 ============
const clearTargets = reactive({
  cache: false,
  favorites: false,
  wrongs: false,
  history: false,
  stats: false
})

const onClearLocal = async () => {
  const chosen: string[] = []
  if (clearTargets.cache) chosen.push('题库缓存')
  if (clearTargets.favorites) chosen.push('收藏')
  if (clearTargets.wrongs) chosen.push('错题')
  if (clearTargets.history) chosen.push('历史题目')
  if (clearTargets.stats) chosen.push('抽卡统计')
  if (chosen.length === 0) {
    ElMessage.warning('请先勾选要清空的项目')
    return
  }
  try {
    await ElMessageBox.confirm(`将清空：${chosen.join('、')}。此操作不可恢复，确定继续？`, '危险操作', {
      type: 'error',
      confirmButtonText: '确认清空',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  if (clearTargets.cache) invalidateQuestionCache()
  if (clearTargets.favorites) clearFavorites()
  if (clearTargets.wrongs) clearWrongs()
  if (clearTargets.history) clearHistory()
  if (clearTargets.stats) clearDrawStats()
  Object.keys(clearTargets).forEach((k) => ((clearTargets as Record<string, boolean>)[k] = false))
  refreshStorage()
  ElMessage.success(`已清空：${chosen.join('、')}`)
}

// ============ 本地存储用量 ============
interface StorageRow {
  key: string
  label: string
  count: string
  size: number
}

const storageRows = ref<StorageRow[]>([])
const storageTotal = ref(0)

const KEY_LABELS: Record<string, string> = {
  'quizcard:settings': '应用设置',
  'quizcard:favorites': '收藏题目',
  'quizcard:wrongs': '错题',
  'quizcard:drawnIds': '本轮抽卡',
  'quizcard:drawCount': '抽卡总次数',
  'quizcard:drawLog': '抽卡时间日志',
  'quizcard:drawHistory': '历史题目',
  'quizcard:cache:categories': '分类列表缓存',
  'quizcard:cache:countByCategory': '分类题目数缓存',
  'quizcard:darkMode': '旧版主题设置',
  'quizcard:noRepeatDraw': '旧版抽卡设置',
  'quizcard:currentCategoryId': '旧版分类筛选',
  'quizcard:currentType': '旧版题型筛选'
}

const formatSize = (bytes: number) =>
  bytes < 1024 ? `${bytes} B` : `${(bytes / 1024).toFixed(1)} KB`

const refreshStorage = () => {
  const rows: StorageRow[] = []
  let total = 0
  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i)
    if (!key || !key.startsWith('quizcard:')) continue
    const value = localStorage.getItem(key) ?? ''
    total += value.length
    let count = '-'
    try {
      const parsed = JSON.parse(value)
      if (Array.isArray(parsed)) count = `${parsed.length} 条`
      else if (parsed && typeof parsed === 'object' && Array.isArray((parsed as { v?: unknown }).v)) {
        // 题库缓存形如 { v: T[], t: 时间戳 }
        count = `${((parsed as { v: unknown[] }).v).length} 条`
      } else if (typeof parsed === 'number') count = String(parsed)
    } catch {
      count = '-'
    }
    rows.push({ key, label: KEY_LABELS[key] || key.replace('quizcard:', ''), count, size: value.length })
  }
  rows.sort((a, b) => b.size - a.size)
  storageRows.value = rows
  storageTotal.value = total
}

onMounted(async () => {
  refreshStorage()
  try {
    categories.value = await listCategoriesCached()
  } catch {
    // 分类接口失败时不阻塞设置页
  }
  try {
    aiConfig.value = await getAiConfig()
  } catch {
    aiConfig.value = null
  }
})
</script>

<style scoped>
.head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.head-title {
  font-weight: 600;
  white-space: nowrap;
}
.search {
  max-width: 320px;
  margin-left: auto;
}
.auto-save {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}
.section h4 {
  margin: 4px 0 8px;
  font-size: 15px;
}
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 10px 0;
  border-bottom: 1px dashed var(--el-border-color-lighter);
}
.row:last-child {
  border-bottom: none;
}
.text {
  min-width: 0;
}
.label {
  font-size: 14px;
  font-weight: 600;
}
.desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
  margin-top: 2px;
}
.ctrl {
  width: 200px;
  flex-shrink: 0;
}
.colors {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  flex-shrink: 0;
}
.color-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  color: var(--el-text-color-regular);
}
.color-item.active {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
}
.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  display: inline-block;
}
.volume {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
.volume-value {
  width: 30px;
  text-align: right;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.btn-group {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
.clear-row {
  align-items: flex-start;
}
.clear-actions {
  display: flex;
  align-items: center;
  gap: 6px 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
  max-width: 420px;
}
.storage-row {
  border-bottom: none;
}
</style>
