import http from './http'

export interface AiConfig {
  model: string
  streamingSupported: boolean
}

/** 后端当前 AI 配置（模型名等，设置页只读展示） */
export const getAiConfig = (): Promise<AiConfig> => http.get('/ai/config')
