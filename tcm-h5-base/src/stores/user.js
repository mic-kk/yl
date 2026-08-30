import { defineStore } from 'pinia'
import request from '@/utils/request'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('tcm_token') || '',
    userInfo: JSON.parse(localStorage.getItem('tcm_user_info') || 'null'),
  }),
  actions: {
    async login(form) {
      const res = await request.post('/auth/login', form)
      this.token = res.data.token
      this.userInfo = res.data
      localStorage.setItem('tcm_token', this.token)
      localStorage.setItem('tcm_user_info', JSON.stringify(this.userInfo))
    },
    async fetchInfo() {
      const res = await request.get('/auth/info')
      this.userInfo = res.data
      localStorage.setItem('tcm_user_info', JSON.stringify(this.userInfo))
    },
    logout() {
      try {
        request.post('/auth/logout')
      } catch (e) {
        // 忽略登出接口异常
      }
      this.resetAuth()
    },
    resetAuth() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('tcm_token')
      localStorage.removeItem('tcm_user_info')
    },
  },
})
