// Web Audio API 程序生成音效，无外部音频文件依赖
import { useSettings } from '@/composables/useSettings'

let ctx: AudioContext | null = null

function ac(): AudioContext {
  if (!ctx) ctx = new AudioContext()
  // 浏览器自动播放策略：挂起状态时尝试恢复
  if (ctx.state === 'suspended') void ctx.resume()
  return ctx
}

/** 当前音量系数：设置里关闭音效或音量为 0 时返回 0（静音） */
function volumeScale(): number {
  const { settings } = useSettings()
  if (!settings.soundEnabled) return 0
  return Math.max(0, Math.min(100, settings.soundVolume)) / 100
}

/** 翻卡 whoosh 音效：滤波噪声扫频 */
export function playFlip() {
  const scale = volumeScale()
  if (scale <= 0) return
  try {
    const a = ac()
    const t = a.currentTime
    const dur = 0.25
    const buf = a.createBuffer(1, Math.floor(a.sampleRate * dur), a.sampleRate)
    const d = buf.getChannelData(0)
    for (let i = 0; i < d.length; i++) d[i] = (Math.random() * 2 - 1) * (1 - i / d.length)
    const src = a.createBufferSource()
    src.buffer = buf
    const f = a.createBiquadFilter()
    f.type = 'bandpass'
    f.Q.value = 1.5
    f.frequency.setValueAtTime(400, t)
    f.frequency.exponentialRampToValueAtTime(2400, t + dur)
    const g = a.createGain()
    g.gain.setValueAtTime(0.18 * scale, t)
    g.gain.exponentialRampToValueAtTime(0.001, t + dur)
    src.connect(f).connect(g).connect(a.destination)
    src.start()
  } catch {
    // 音频不可用时静默
  }
}

/** 抽到新题的 pop 短音 */
export function playPop() {
  const scale = volumeScale()
  if (scale <= 0) return
  try {
    const a = ac()
    const t = a.currentTime
    const o = a.createOscillator()
    const g = a.createGain()
    o.type = 'sine'
    o.frequency.setValueAtTime(520, t)
    o.frequency.exponentialRampToValueAtTime(880, t + 0.08)
    g.gain.setValueAtTime(0.12 * scale, t)
    g.gain.exponentialRampToValueAtTime(0.001, t + 0.15)
    o.connect(g).connect(a.destination)
    o.start(t)
    o.stop(t + 0.15)
  } catch {
    // 静默
  }
}

/** 翻页音效：上一题/下一题切换时的短促下滑 tick */
export function playPage() {
  const { settings } = useSettings()
  if (!settings.pageSoundEnabled) return
  const scale = volumeScale()
  if (scale <= 0) return
  try {
    const a = ac()
    const t = a.currentTime
    const o = a.createOscillator()
    const g = a.createGain()
    o.type = 'triangle'
    o.frequency.setValueAtTime(720, t)
    o.frequency.exponentialRampToValueAtTime(400, t + 0.1)
    g.gain.setValueAtTime(0.1 * scale, t)
    g.gain.exponentialRampToValueAtTime(0.001, t + 0.12)
    o.connect(g).connect(a.destination)
    o.start(t)
    o.stop(t + 0.12)
  } catch {
    // 静默
  }
}
