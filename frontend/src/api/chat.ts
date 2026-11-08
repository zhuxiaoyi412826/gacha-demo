// AI 聊天接口：POST + fetch 读取 SSE，支持携带历史上下文与流式/非流式切换

export interface ChatHistoryMessage {
  role: 'user' | 'assistant'
  content: string
}

export interface ChatRequest {
  message: string
  history?: ChatHistoryMessage[]
}

export interface ChatStreamHandlers {
  onMessage: (delta: string) => void
}

/** 流式聊天：逐 token 回调；失败时抛出异常 */
export async function streamChat(payload: ChatRequest, handlers: ChatStreamHandlers): Promise<void> {
  const resp = await fetch('/api/chat/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
  if (!resp.ok) throw new Error(`AI 请求失败：HTTP ${resp.status}`)
  if (!resp.body) throw new Error('当前浏览器不支持流式读取')

  const reader = resp.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n')
    let idx = buffer.indexOf('\n\n')
    while (idx >= 0) {
      const frame = buffer.slice(0, idx)
      buffer = buffer.slice(idx + 2)
      // Spring SSE 每条事件形如 "data:xxx"，一个事件可能由多行 data 组成
      const data = frame
        .split('\n')
        .filter((line) => line.startsWith('data:'))
        .map((line) => line.slice(5).replace(/^ /, ''))
        .join('\n')
      if (data) handlers.onMessage(data)
      idx = buffer.indexOf('\n\n')
    }
  }
}

/** 非流式聊天：一次性拿到完整回答 */
export async function chatOnce(payload: ChatRequest): Promise<string> {
  const resp = await fetch('/api/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
  const body = await resp.json()
  if (body.code !== 200) throw new Error(body.msg || 'AI 请求失败')
  return body.data as string
}
