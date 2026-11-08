<template>
  <div class="page-container">
    <el-card>
      <div class="header-row">
        <div class="flex-row filter-row">
          <span class="label">题目分类：</span>
          <el-select
            v-model="currentCategoryId"
            placeholder="全部题目"
            clearable
            :value-on-clear="null"
            style="width: 200px"
            @change="onFilterChange"
          >
            <el-option v-for="c in categories" :key="c.id" :label="c.categoryName" :value="c.id" />
          </el-select>
          <span class="label">题型：</span>
          <el-select
            v-model="currentType"
            placeholder="全部题型"
            clearable
            style="width: 160px"
            @change="onFilterChange"
          >
            <el-option label="全部题型" :value="''" />
            <el-option label="单选" value="单选" />
            <el-option label="多选" value="多选" />
            <el-option label="简答" value="简答" />
          </el-select>
          <el-tag type="info">已抽 {{ drawCount }}</el-tag>
          <el-tag v-if="settings.noRepeatDraw" type="success" effect="plain">不重复抽卡</el-tag>
          <el-tag v-if="settings.drawMode === 'sequential'" type="warning" effect="plain">顺序抽卡</el-tag>
          <el-tag v-else-if="settings.drawMode === 'unseen'" type="info" effect="plain">只抽未抽过</el-tag>
        </div>
        <div>
          <el-button type="primary" :icon="Refresh" @click="drawNext">
            抽卡
          </el-button>
          <el-button @click="resetRound" :disabled="drawnIds.length === 0">重置本轮</el-button>
        </div>
      </div>
    </el-card>

    <div class="card-wrap mt-24">
      <div class="flip-stage" :class="{ 'no-anim': !settings.animationsEnabled }">
        <FlipCard
          v-if="currentQuestion"
          :key="currentQuestion.id"
          :flipped="flipped"
          :front-style="frontStyle"
          :back-style="backStyle"
          @flip="onFlip"
        >
          <template #front>
            <div class="card-content">
              <div class="card-tag">
                <el-tag>{{ currentQuestion.type }}</el-tag>
                <el-tag v-if="isFavorite(currentQuestion.id)" type="warning" effect="plain">已收藏</el-tag>
                <el-tag v-if="isWrong(currentQuestion.id)" type="danger" effect="plain">错题</el-tag>
                <el-button class="copy-btn" link type="primary" size="small" @click.stop="onCopyQuestion">复制题目</el-button>
              </div>
              <h2 class="card-title">{{ currentQuestion.title }}</h2>
              <div v-if="currentQuestion.options && currentQuestion.type !== '简答'" class="options">
                <div v-for="(opt, i) in parsedOptions" :key="i" class="option-item">
                  <span class="opt-label">{{ opt.label }}.</span> {{ opt.text }}
                </div>
              </div>
              <div class="hint">点击卡片查看答案 →</div>
            </div>
          </template>
          <template #back>
            <div class="card-content card-back-content">
              <h3>参考答案<el-button class="copy-ans" link type="primary" size="small" @click.stop="onCopyAnswer">复制答案</el-button></h3>
              <div class="answer" :style="answerStyle">
                <div v-if="settings.markdownEnabled" class="markdown-body" v-html="renderMarkdown(currentQuestion.answer)"></div>
                <div v-else class="plain-text">{{ currentQuestion.answer }}</div>
              </div>
              <div v-if="currentQuestion.analysis" class="analysis" :style="answerStyle">
                <h4>解析</h4>
                <div v-if="settings.markdownEnabled" class="markdown-body" v-html="renderMarkdown(currentQuestion.analysis)"></div>
                <div v-else class="plain-text">{{ currentQuestion.analysis }}</div>
              </div>
              <div class="hint">点击翻回 ←</div>
            </div>
          </template>
        </FlipCard>
        <div v-else class="empty">
          <el-empty description="点击「抽卡」开始抽题" />
        </div>
      </div>

      <div v-if="currentQuestion" class="actions mt-16">
        <el-button @click="goPrev" :disabled="historyIndex <= 0">上一题</el-button>
        <el-button type="primary" plain @click="goNext">下一题</el-button>
        <el-button @click="toggleFavorite(currentQuestion.id)" :type="isFavorite(currentQuestion.id) ? 'warning' : 'default'">
          {{ isFavorite(currentQuestion.id) ? '取消收藏' : '收藏' }}
        </el-button>
        <el-button @click="toggleWrong(currentQuestion.id)" :type="isWrong(currentQuestion.id) ? 'danger' : 'default'">
          {{ isWrong(currentQuestion.id) ? '取消错题' : '标记错题' }}
        </el-button>
        <el-button :icon="Share" :loading="sharing" @click="onShare">分享卡片</el-button>
      </div>
    </div>

    <!-- 分享卡片：隐藏渲染，html2canvas 截图导出 PNG（固定浅色风格，与主题无关） -->
    <div v-if="currentQuestion" ref="shareCardRef" class="share-card" aria-hidden="true">
      <div class="sc-header">
        <span class="sc-logo">🎯 题目抽卡</span>
        <span class="sc-meta">{{ shareCategoryLabel }} · {{ currentQuestion.type }}</span>
      </div>
      <div class="sc-title">{{ currentQuestion.title }}</div>
      <div v-if="currentQuestion.options && currentQuestion.type !== '简答'" class="sc-options">
        <div v-for="(opt, i) in parsedOptions" :key="i" class="sc-opt">
          <b>{{ opt.label }}.</b> {{ opt.text }}
        </div>
      </div>
      <div class="sc-section">
        <div class="sc-label">参考答案</div>
        <div class="sc-md" v-html="renderMarkdown(currentQuestion.answer)"></div>
      </div>
      <div v-if="currentQuestion.analysis" class="sc-section">
        <div class="sc-label">解析</div>
        <div class="sc-md sc-analysis" v-html="renderMarkdown(currentQuestion.analysis)"></div>
      </div>
      <div class="sc-footer">{{ new Date().toLocaleString('zh-CN') }} · 打开「题目抽卡」一起学习</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Refresh, Share } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import html2canvas from 'html2canvas'
import FlipCard from '@/components/FlipCard.vue'
import { listCategoriesCached } from '@/api/category'
import { pickQuestion } from '@/api/question'
import { useDrawStats } from '@/composables/useDrawStats'
import { FONT_SIZE_MAP, LINE_HEIGHT_MAP, useSettings } from '@/composables/useSettings'
import { useDarkMode } from '@/composables/useDarkMode'
import { renderMarkdown } from '@/utils/markdown'
import { playFlip, playPage, playPop } from '@/utils/sound'
import type { Category, Question, QuestionType } from '@/types'

const { isDark } = useDarkMode()
// 卡片配色跟随主题：dark 下用深色系避免刺眼
const frontStyle = computed(() => ({ background: isDark.value ? '#1d1e1f' : '#fff' }))
const backStyle = computed(() => ({ background: isDark.value ? '#16232f' : '#f0f9ff' }))

const categories = ref<Category[]>([])
const { settings } = useSettings()
// 新建一轮时套用设置里的默认筛选
const currentCategoryId = ref<number | null>(settings.defaultCategoryId)
const currentType = ref<QuestionType | ''>(settings.defaultType)
const currentQuestion = ref<Question | null>(null)
const flipped = ref(false)
const history = ref<Question[]>([])
const historyIndex = ref(-1)
/** 顺序抽卡：记录上一题的 ID，用于请求「之后的一题」 */
const lastDrawnId = ref<number | null>(null)

const {
  drawCount,
  drawnIds,
  drawHistory,
  isFavorite,
  isWrong,
  toggleFavorite,
  toggleWrong,
  recordDraw,
  resetDrawnIds
} = useDrawStats()

/** 答案排版：字号 + 行距（设置页可调） */
const answerStyle = computed(() => ({
  fontSize: FONT_SIZE_MAP[settings.answerFontSize],
  lineHeight: String(LINE_HEIGHT_MAP[settings.answerLineHeight])
}))

const parsedOptions = computed(() => {
  if (!currentQuestion.value?.options) return []
  try {
    const arr = JSON.parse(currentQuestion.value.options)
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
})

// ============ 分享卡片 ============
const shareCardRef = ref<HTMLElement>()
const sharing = ref(false)
const shareCategoryLabel = computed(() => {
  const cid = currentQuestion.value?.categoryId
  if (cid == null) return '默认分类'
  return categories.value.find((c) => c.id === cid)?.categoryName || '默认分类'
})

/** 截取隐藏分享卡片 DOM 生成 PNG 下载 */
const onShare = async () => {
  if (!shareCardRef.value || !currentQuestion.value) return
  sharing.value = true
  try {
    const canvas = await html2canvas(shareCardRef.value, { scale: 2, backgroundColor: '#ffffff' })
    const a = document.createElement('a')
    a.href = canvas.toDataURL('image/png')
    a.download = `question-${currentQuestion.value.id}.png`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    ElMessage.success('分享卡片已保存为 PNG')
  } catch (err) {
    ElMessage.error('生成分享卡片失败：' + (err as Error).message)
  } finally {
    sharing.value = false
  }
}

const fetchCategories = async () => {
  categories.value = await listCategoriesCached()
}

/** 翻卡：切换状态 + 播放 whoosh 音效 */
const onFlip = () => {
  clearAutoFlip()
  flipped.value = !flipped.value
  playFlip()
}

/** 复制文本到剪贴板（clipboard API 不可用时降级 textarea） */
const copyText = async (text: string, label: string) => {
  try {
    await navigator.clipboard.writeText(text)
  } catch {
    const ta = document.createElement('textarea')
    ta.value = text
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
  }
  ElMessage.success(`${label}已复制到剪贴板`)
}

/** 复制题目：题干 + 选项纯文本 */
const onCopyQuestion = () => {
  const q = currentQuestion.value
  if (!q) return
  let text = q.title
  if (q.options && q.type !== '简答') {
    const opts = parsedOptions.value.map((o) => `${o.label}. ${o.text}`).join('\n')
    if (opts) text += '\n' + opts
  }
  copyText(text, '题目')
}

/** 复制答案：参考答案 + 解析纯文本 */
const onCopyAnswer = () => {
  const q = currentQuestion.value
  if (!q) return
  copyText(q.answer + (q.analysis ? `\n\n解析：\n${q.analysis}` : ''), '答案')
}

const onFilterChange = () => {
  currentQuestion.value = null
  history.value = []
  historyIndex.value = -1
  flipped.value = false
  lastDrawnId.value = null
  resetDrawnIds()
}

/** 需要排除的题目：只抽未抽过 = 历史 + 本轮；不重复抽卡 = 本轮 */
const buildExcludeIds = (): number[] => {
  if (settings.drawMode === 'unseen') {
    return Array.from(new Set([...drawHistory.value, ...drawnIds.value]))
  }
  return settings.noRepeatDraw ? [...drawnIds.value] : []
}

// 自动翻面定时器 / 语音朗读
let autoFlipTimer: number | undefined
const clearAutoFlip = () => {
  if (autoFlipTimer) {
    clearTimeout(autoFlipTimer)
    autoFlipTimer = undefined
  }
}

/** 抽到后自动朗读题干（浏览器 TTS） */
const speakQuestion = (text: string) => {
  if (!('speechSynthesis' in window)) return
  window.speechSynthesis.cancel()
  const u = new SpeechSynthesisUtterance(text)
  u.lang = 'zh-CN'
  window.speechSynthesis.speak(u)
}

const drawNext = async () => {
  clearAutoFlip()
  const excludeIds = buildExcludeIds()
  try {
    const q = await pickQuestion({
      categoryId: currentCategoryId.value,
      type: currentType.value || null,
      excludeIds,
      mode: settings.drawMode === 'sequential' ? 'sequential' : 'random',
      afterId: settings.drawMode === 'sequential' ? lastDrawnId.value : null
    })
    if (!q) {
      if (settings.drawMode === 'unseen') {
        ElMessage.warning(`当前筛选下的题目都已抽过（共 ${excludeIds.length} 道），可在设置里切换抽卡模式`)
      } else if (settings.noRepeatDraw && drawnIds.value.length > 0) {
        ElMessage.warning(`本轮题目已抽完（共 ${drawnIds.value.length} 道），点击「重置本轮」后可继续`)
      } else {
        ElMessage.warning('当前筛选下暂无题目')
      }
      return
    }
    lastDrawnId.value = q.id
    // 截断后续历史
    history.value = history.value.slice(0, historyIndex.value + 1)
    history.value.push(q)
    historyIndex.value = history.value.length - 1
    currentQuestion.value = q
    flipped.value = false
    recordDraw(q.id)
    playPop() // 新题入场音效
    if (settings.autoSpeak) {
      speakQuestion(q.title)
    }
    if (settings.autoFlip) {
      autoFlipTimer = window.setTimeout(() => {
        if (currentQuestion.value?.id === q.id && !flipped.value) {
          flipped.value = true
          playFlip()
        }
      }, 3000)
    }
  } catch {
    // axios 拦截器已提示
  }
}

const goPrev = () => {
  if (historyIndex.value > 0) {
    historyIndex.value -= 1
    currentQuestion.value = history.value[historyIndex.value]
    flipped.value = false
    playPage()
  }
}
// 下一题：历史中还有后续题目则前进浏览；已到最新一条则随机抽一道新题
const goNext = async () => {
  if (historyIndex.value < history.value.length - 1) {
    historyIndex.value += 1
    currentQuestion.value = history.value[historyIndex.value]
    flipped.value = false
    playPage()
  } else {
    await drawNext()
  }
}

const resetRound = () => {
  clearAutoFlip()
  resetDrawnIds()
  history.value = []
  historyIndex.value = -1
  currentQuestion.value = null
  flipped.value = false
  lastDrawnId.value = null
  ElMessage.success('已重置本轮抽卡历史')
}

/** 快捷键：空格=抽卡/翻面，←/→=上一题/下一题，1=收藏，2=错题（设置里可关闭） */
const onKeydown = (e: KeyboardEvent) => {
  if (!settings.shortcutsEnabled) return
  const el = e.target as HTMLElement | null
  if (
    el &&
    (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA' || el.tagName === 'SELECT' || el.isContentEditable)
  ) {
    return
  }
  if (e.code === 'Space') {
    e.preventDefault()
    if (!currentQuestion.value) {
      void drawNext()
    } else if (!flipped.value) {
      onFlip()
    } else {
      void drawNext()
    }
  } else if (e.key === 'ArrowLeft') {
    e.preventDefault()
    goPrev()
  } else if (e.key === 'ArrowRight') {
    e.preventDefault()
    void goNext()
  } else if (e.key === '1') {
    if (currentQuestion.value) toggleFavorite(currentQuestion.value.id)
  } else if (e.key === '2') {
    if (currentQuestion.value) toggleWrong(currentQuestion.value.id)
  }
}

watch(currentQuestion, () => { flipped.value = false })

onMounted(() => {
  fetchCategories()
  window.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  clearAutoFlip()
  if ('speechSynthesis' in window) window.speechSynthesis.cancel()
})
</script>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
.filter-row {
  gap: 8px;
}
.label {
  font-weight: 600;
}
.card-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.flip-stage {
  width: 720px;
  height: 504px;
  max-width: 100%;
}
/* 换题重挂载（:key=question.id）时触发入场动画，keyframes 定义在全局 main.css */
.flip-stage :deep(.flip-card) {
  animation: card-in 0.35s ease;
}
/* 设置里关闭动画后：去掉入场动画与翻转过渡 */
.no-anim :deep(.flip-card) {
  animation: none;
}
.no-anim :deep(.flip-card-inner) {
  transition: none;
}
/* 关闭 Markdown 渲染时的纯文本排版 */
.plain-text {
  white-space: pre-wrap;
  word-break: break-word;
}
.card-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  gap: 12px;
}
.card-tag {
  display: flex;
  gap: 8px;
  align-items: center;
}
.copy-btn {
  margin-left: auto;
}
.copy-ans {
  margin-left: 8px;
  vertical-align: 2px;
}
.card-title {
  font-size: 18px;
  font-weight: 600;
  line-height: 1.6;
}
.options {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}
.option-item {
  padding: 6px 10px;
  background: var(--el-fill-color-light);
  border-radius: 6px;
}
.opt-label {
  font-weight: 700;
  margin-right: 8px;
}
.answer {
  font-size: 16px;
  line-height: 1.6;
  background: var(--el-bg-color);
  padding: 12px;
  border-radius: 6px;
}
.analysis {
  margin-top: 8px;
  color: var(--el-text-color-regular);
  background: var(--el-bg-color);
  padding: 12px;
  border-radius: 6px;
}
/* 背面内容过长时可滚动，避免撑破卡片 */
.card-back-content {
  overflow-y: auto;
}
/* Markdown 渲染排版：自动换行 + 基础元素样式 */
:deep(.markdown-body) {
  word-break: break-word;
  overflow-wrap: anywhere;
  line-height: 1.7;
}
:deep(.markdown-body p) {
  margin: 0 0 8px;
}
:deep(.markdown-body p:last-child) {
  margin-bottom: 0;
}
:deep(.markdown-body ul),
:deep(.markdown-body ol) {
  margin: 4px 0 8px;
  padding-left: 22px;
}
:deep(.markdown-body li) {
  margin-bottom: 4px;
}
:deep(.markdown-body code) {
  background: var(--el-fill-color-light);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.9em;
  color: #d63384;
}
:deep(.markdown-body pre) {
  background: #2d2d2d;
  color: #f8f8f2;
  padding: 10px 12px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 8px 0;
}
:deep(.markdown-body pre code) {
  background: transparent;
  color: inherit;
  padding: 0;
}
:deep(.markdown-body strong) {
  color: var(--el-text-color-primary);
}
:deep(.markdown-body h1),
:deep(.markdown-body h2),
:deep(.markdown-body h3),
:deep(.markdown-body h4) {
  margin: 10px 0 6px;
  line-height: 1.4;
}
:deep(.markdown-body blockquote) {
  margin: 8px 0;
  padding: 4px 12px;
  border-left: 3px solid #409eff;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
}
:deep(.markdown-body table) {
  border-collapse: collapse;
  margin: 8px 0;
  width: 100%;
}
:deep(.markdown-body th),
:deep(.markdown-body td) {
  border: 1px solid var(--el-border-color);
  padding: 4px 8px;
  font-size: 0.92em;
}
.hint {
  margin-top: auto;
  text-align: center;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
}
.empty {
  width: 720px;
  height: 504px;
  display: flex;
  align-items: center;
  justify-content: center;
  max-width: 100%;
}

/* ============ 分享卡片（隐藏渲染，仅供截图；固定浅色保证导出效果一致） ============ */
.share-card {
  position: fixed;
  left: -10000px;
  top: 0;
  width: 680px;
  padding: 28px 32px;
  background: linear-gradient(135deg, #eef4ff 0%, #ffffff 60%);
  border: 1px solid #d9e6ff;
  border-radius: 14px;
  color: #2c3e50;
  font-size: 15px;
  line-height: 1.7;
  z-index: -1;
}
.sc-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}
.sc-logo {
  font-size: 18px;
  font-weight: 700;
  color: #409eff;
}
.sc-meta {
  font-size: 12px;
  color: #7a8ba0;
  background: #eaf2ff;
  border-radius: 10px;
  padding: 2px 10px;
}
.sc-title {
  font-size: 19px;
  font-weight: 700;
  line-height: 1.6;
  margin-bottom: 12px;
}
.sc-options {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
}
.sc-opt {
  background: #f5f8ff;
  border: 1px solid #e3edff;
  border-radius: 6px;
  padding: 6px 10px;
}
.sc-opt b {
  color: #409eff;
  margin-right: 4px;
}
.sc-section {
  margin-top: 10px;
}
.sc-label {
  font-size: 13px;
  font-weight: 700;
  color: #409eff;
  margin-bottom: 6px;
}
.sc-md {
  background: #ffffff;
  border: 1px solid #e8eef7;
  border-radius: 8px;
  padding: 10px 14px;
}
.sc-md p {
  margin: 0 0 6px;
}
.sc-md p:last-child {
  margin-bottom: 0;
}
.sc-analysis {
  color: #5c6b7a;
  font-size: 14px;
}
.sc-footer {
  margin-top: 16px;
  font-size: 12px;
  color: #9aa7b5;
  text-align: right;
}
</style>
