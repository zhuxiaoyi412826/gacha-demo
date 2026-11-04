// AI 聊天 SSE 封装：基于原生 EventSource（GET 接口）
// 调用方传入回调，逐字返回 token

export interface ChatStreamHandlers {
  onMessage: (delta: string) => void
  onError?: (err: Event) => void
  onClose?: () => void
}

export function streamChat(message: string, handlers: ChatStreamHandlers): EventSource {
  const url = `/api/chat/stream?message=${encodeURIComponent(message)}`
  const es = new EventSource(url)
  es.onmessage = (e) => {
    // Spring SSE 默认每条 message 分隔，e.data 即当前 token
    handlers.onMessage(e.data)
  }
  es.onerror = (e) => {
    es.close()
    handlers.onError?.(e)
    handlers.onClose?.()
  }
  // 当 SpringAI 流结束时，连接会关闭，触发 onerror（EventSource 把连接关闭视为错误）
  // 我们在 onerror 里统一 close
  return es
}
