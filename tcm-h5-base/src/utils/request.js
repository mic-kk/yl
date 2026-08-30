import axios from 'axios'
import { showToast } from 'vant'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 15000,
})

request.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = userStore.token
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res
    }
    if (res.code === 2001) {
      // 未登录/过期：登出并回登录页
      useUserStore().resetAuth()
      router.push('/login')
      return Promise.reject(new Error(res.msg))
    }
    showToast(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  (error) => {
    const res = error.response?.data
    if (error.response?.status === 401 || res?.code === 2001) {
      useUserStore().resetAuth()
      router.push('/login')
    } else {
      showToast(res?.msg || error.message || '网络错误')
    }
    return Promise.reject(error)
  },
)

export default request
