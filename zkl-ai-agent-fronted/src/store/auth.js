import { defineStore } from 'pinia'
import { api } from '../utils/api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    token: localStorage.getItem('token') || null,
    isLoggedIn: false
  }),

  getters: {
    isAuthenticated: (state) => !!state.token && !!state.user,
    userInfo: (state) => state.user
  },

  actions: {
    // 设置token和用户信息
    setAuth(token, user) {
      this.token = token
      this.user = user
      this.isLoggedIn = true
      localStorage.setItem('token', token)
      
      // 设置axios默认请求头
      if (token) {
        import('axios').then(axios => {
          axios.default.defaults.headers.common['satoken'] = token
        })
      }
    },

    // 清除认证信息
    clearAuth() {
      this.token = null
      this.user = null
      this.isLoggedIn = false
      localStorage.removeItem('token')
      
      // 清除axios默认请求头
      import('axios').then(axios => {
        delete axios.default.defaults.headers.common['satoken']
      })
    },

    // 登录
    async login(loginData) {
      try {
        const response = await api.auth.login(loginData)
        if (response.success && response.data) {
          this.setAuth(response.data.accessToken, {
            userId: response.data.userId,
            username: response.data.username,
            nickName: response.data.nickName,
            userType: response.data.userType
          })
          return response
        } else {
          throw new Error(response.message || '登录失败')
        }
      } catch (error) {
        console.error('登录失败:', error)
        throw error
      }
    },

    // 注册
    async register(registerData) {
      try {
        const response = await api.auth.register(registerData)
        return response
      } catch (error) {
        console.error('注册失败:', error)
        throw error
      }
    },

    // 退出登录
    async logout() {
      try {
        if (this.token) {
          await api.auth.logout()
        }
      } catch (error) {
        console.error('退出登录失败:', error)
      } finally {
        this.clearAuth()
      }
    },

    // 获取用户信息
    async fetchUserInfo() {
      try {
        if (!this.token) return null
        
        const response = await api.auth.getUserInfo()
        if (response.success && response.data) {
          this.user = response.data
          this.isLoggedIn = true
          return response.data
        }
      } catch (error) {
        console.error('获取用户信息失败:', error)
        // 如果获取用户信息失败，可能token已过期，清除认证信息
        this.clearAuth()
        throw error
      }
    },

    // 初始化认证状态
    async initAuth() {
      if (this.token) {
        // 设置axios默认请求头
        import('axios').then(axios => {
          axios.default.defaults.headers.common['satoken'] = this.token
        })
        
        try {
          await this.fetchUserInfo()
        } catch (error) {
          // 如果获取用户信息失败，清除本地存储的token
          this.clearAuth()
        }
      }
    }
  },

  // 启用持久化
  persist: {
    key: 'auth-store',
    storage: localStorage,
    paths: ['token', 'user', 'isLoggedIn']
  }
})