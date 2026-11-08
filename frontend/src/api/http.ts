import axios, { AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types'

const http = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 响应拦截器：解包 ApiResponse，业务异常抛出 Error
http.interceptors.response.use(
  // 返回解包后的业务数据（而非 AxiosResponse），因此显式标注 any
  (resp): any => {
    const body = resp.data as ApiResponse
    if (body && typeof body.code === 'number') {
      if (body.code === 200) {
        return body.data
      }
      ElMessage.error(body.msg || '请求失败')
      return Promise.reject(new Error(body.msg || '请求失败'))
    }
    // 非标准 ApiResponse（如直接二进制等），直接返回
    return body
  },
  (err: AxiosError) => {
    const msg = (err.response?.data as ApiResponse)?.msg || err.message || '网络异常'
    ElMessage.error(msg)
    return Promise.reject(err)
  }
)

export default http
