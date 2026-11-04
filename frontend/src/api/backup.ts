import http from './http'
import type { Category, Question } from '@/types'

/** 备份文件结构（整库快照） */
export interface BackupPayload {
  version: number
  exportedAt: string
  categories: Category[]
  questions: Question[]
}

export interface RestoreResult {
  categories: number
  questions: number
}

/** 导出整库快照 */
export const exportBackup = (): Promise<BackupPayload> => http.get('/backup/export')

/** 恢复整库快照（覆盖现有数据） */
export const restoreBackup = (payload: BackupPayload): Promise<RestoreResult> =>
  http.post('/backup/restore', payload)
