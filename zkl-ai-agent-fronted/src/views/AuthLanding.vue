<template>
  <div class="auth-landing">
    <div class="auth-container">
      <div class="auth-header">
        <h1 class="auth-title">疾控监督AI 智能体平台</h1>
        <p class="auth-subtitle">欢迎使用疾控监督智能AI助手服务</p>
      </div>
      
      <div class="auth-content">
        <div class="auth-tabs">
          <button 
            :class="['auth-tab', { active: activeTab === 'login' }]"
            @click="activeTab = 'login'"
          >
            登录
          </button>
          <button 
            :class="['auth-tab', { active: activeTab === 'register' }]"
            @click="activeTab = 'register'"
          >
            注册
          </button>
        </div>
        
        <!-- 登录表单 -->
        <div v-if="activeTab === 'login'" class="auth-form-container">
          <div class="test-account-tip">
            <div class="tip-icon">💡</div>
            <div class="tip-content">
              <p class="tip-title">测试账号</p>
              <p class="tip-text">已为您预填充测试账号信息，可直接登录体验</p>
            </div>
          </div>
          <form @submit.prevent="handleLogin" class="auth-form">
            <div class="form-group">
              <label for="username" class="form-label">用户名</label>
              <input
                id="username"
                v-model="loginForm.username"
                type="text"
                class="form-input"
                placeholder="请输入用户名"
                :disabled="loading"
                required
              />
            </div>
            
            <div class="form-group">
              <label for="password" class="form-label">密码</label>
              <input
                id="password"
                v-model="loginForm.password"
                type="password"
                class="form-input"
                placeholder="请输入密码"
                :disabled="loading"
                required
              />
            </div>
            
            <div class="form-group">
              <label for="captcha" class="form-label">验证码</label>
              <div class="captcha-group">
                <input
                  id="captcha"
                  v-model="loginForm.captcha"
                  type="text"
                  class="form-input captcha-input"
                  placeholder="请输入验证码"
                  :disabled="loading"
                  required
                />
                <div class="captcha-image" @click="refreshLoginCaptcha">
                  <img 
                    v-if="loginCaptchaData.captcha" 
                    :src="getCaptchaImageSrc(loginCaptchaData.captcha)" 
                    alt="验证码"
                    @error="onCaptchaImageError"
                    @load="onCaptchaImageLoad"
                    style="display: block;"
                  />
                  <div v-else-if="loading" class="captcha-loading">加载中...</div>
                  <div v-else class="captcha-error" @click="refreshLoginCaptcha">
                    <span>点击刷新验证码</span>
                  </div>
                </div>
              </div>
            </div>
            
            <button
              type="submit"
              class="auth-button login-button"
              :disabled="loading || !isLoginFormValid"
            >
              <span v-if="loading">登录中...</span>
              <span v-else>登录</span>
            </button>
          </form>
        </div>
        
        <!-- 注册表单 -->
        <div v-if="activeTab === 'register'" class="auth-form-container">
          <form @submit.prevent="handleRegister" class="auth-form">
            <div class="form-group">
              <label for="reg-username" class="form-label">用户名</label>
              <input
                id="reg-username"
                v-model="registerForm.username"
                type="text"
                class="form-input"
                placeholder="4-16位字母数字下划线"
                :disabled="loading"
                required
              />
              <div v-if="errors.username" class="error-text">{{ errors.username }}</div>
            </div>
            
            <div class="form-group">
              <label for="reg-nickName" class="form-label">昵称（可选）</label>
              <input
                id="reg-nickName"
                v-model="registerForm.nickName"
                type="text"
                class="form-input"
                placeholder="请输入昵称"
                :disabled="loading"
              />
            </div>
            
            <div class="form-group">
              <label for="reg-password" class="form-label">密码</label>
              <input
                id="reg-password"
                v-model="registerForm.password"
                type="password"
                class="form-input"
                placeholder="8-20位，包含大小写字母和数字"
                :disabled="loading"
                required
              />
              <div v-if="errors.password" class="error-text">{{ errors.password }}</div>
            </div>
            
            <div class="form-group">
              <label for="reg-confirmPassword" class="form-label">确认密码</label>
              <input
                id="reg-confirmPassword"
                v-model="registerForm.confirmPassword"
                type="password"
                class="form-input"
                placeholder="请再次输入密码"
                :disabled="loading"
                required
              />
              <div v-if="errors.confirmPassword" class="error-text">{{ errors.confirmPassword }}</div>
            </div>
            
            <div class="form-group">
              <label for="reg-captcha" class="form-label">验证码</label>
              <div class="captcha-group">
                <input
                  id="reg-captcha"
                  v-model="registerForm.captcha"
                  type="text"
                  class="form-input captcha-input"
                  placeholder="请输入验证码"
                  :disabled="loading"
                  required
                />
                <div class="captcha-image" @click="refreshRegisterCaptcha">
                  <img 
                    v-if="registerCaptchaData.captcha" 
                    :src="getCaptchaImageSrc(registerCaptchaData.captcha)" 
                    alt="验证码"
                    @error="onCaptchaImageError"
                    @load="onCaptchaImageLoad"
                    style="display: block;"
                  />
                  <div v-else-if="loading" class="captcha-loading">加载中...</div>
                  <div v-else class="captcha-error" @click="refreshRegisterCaptcha">
                    <span>点击刷新验证码</span>
                  </div>
                </div>
              </div>
            </div>
            
            <button
              type="submit"
              class="auth-button register-button"
              :disabled="loading || !isRegisterFormValid"
            >
              <span v-if="loading">注册中...</span>
              <span v-else>注册</span>
            </button>
          </form>
        </div>
      </div>
      
      <div class="auth-footer">
        <p>&copy; 2025 天王星数字科技有限公司 保留所有权利. All rights reserved.</p>
      </div>
    </div>
    
    <!-- 消息提示 -->
    <MessageToast
      :show="toast.show"
      :message="toast.message"
      :type="toast.type"
      @hide="hideToast"
    />
  </div>
</template>

<script>
import { useAuthStore } from '../store/auth'
import { api } from '../utils/api'
import MessageToast from '../components/MessageToast.vue'

export default {
  name: 'AuthLanding',
  components: {
    MessageToast
  },
  data() {
    return {
      activeTab: 'login',
      loading: false,
      
      // 登录表单
      loginForm: {
        username: 'admin',
        password: 'Admin123',
        captcha: '',
        captchaKey: ''
      },
      loginCaptchaData: {
        key: '',
        captcha: ''
      },
      
      // 注册表单
      registerForm: {
        username: '',
        nickName: '',
        password: '',
        confirmPassword: '',
        captcha: '',
        captchaKey: ''
      },
      registerCaptchaData: {
        key: '',
        captcha: ''
      },
      
      errors: {},
      toast: {
        show: false,
        message: '',
        type: 'info'
      }
    }
  },
  computed: {
    isLoginFormValid() {
      return this.loginForm.username.trim() && 
             this.loginForm.password.trim() && 
             this.loginForm.captcha.trim()
    },
    
    isRegisterFormValid() {
      return this.registerForm.username.trim() && 
             this.registerForm.password.trim() && 
             this.registerForm.confirmPassword.trim() &&
             this.registerForm.captcha.trim() &&
             Object.keys(this.errors).length === 0
    }
  },
  watch: {
    activeTab(newTab) {
      if (newTab === 'login') {
        this.loadLoginCaptcha()
      } else {
        this.loadRegisterCaptcha()
      }
    },
    
    'registerForm.username'() {
      this.validateUsername()
    },
    'registerForm.password'() {
      this.validatePassword()
    },
    'registerForm.confirmPassword'() {
      this.validateConfirmPassword()
    }
  },
  async mounted() {
    // 测试API连接
    await this.testCaptchaApi()
    await this.loadLoginCaptcha()
  },
  methods: {
    // 加载登录验证码
    async loadLoginCaptcha() {
      try {
        console.log('开始请求验证码...') // 调试日志
        const response = await api.auth.getCaptcha()
        console.log('登录验证码完整响应:', response) // 调试日志
        console.log('响应类型:', typeof response) // 调试日志
        console.log('响应success字段:', response?.success) // 调试日志
        console.log('响应data字段:', response?.data) // 调试日志
        
        if (response && response.success && response.data) {
          this.loginCaptchaData = response.data
          this.loginForm.captchaKey = response.data.key
          console.log('设置验证码数据成功:', this.loginCaptchaData) // 调试日志
          console.log('验证码图片数据长度:', response.data.captcha?.length) // 调试日志
          console.log('验证码图片数据前50字符:', response.data.captcha?.substring(0, 50)) // 调试日志
        } else {
          console.error('验证码响应格式错误:', response) // 调试日志
          this.showToast('获取验证码失败：响应格式错误', 'error')
        }
      } catch (error) {
        console.error('获取验证码网络错误:', error) // 调试日志
        console.error('错误详情:', error.response || error) // 调试日志
        this.showToast(`获取验证码失败：${error.message || '网络错误'}`, 'error')
      }
    },
    
    // 加载注册验证码
    async loadRegisterCaptcha() {
      try {
        const response = await api.auth.getRegisterCaptcha()
        if (response.success && response.data) {
          this.registerCaptchaData = response.data
          this.registerForm.captchaKey = response.data.key
        } else {
          this.showToast('获取验证码失败', 'error')
        }
      } catch (error) {
        console.error('获取验证码失败:', error)
        this.showToast(error.message || '获取验证码失败', 'error')
      }
    },
    
    // 刷新登录验证码
    async refreshLoginCaptcha() {
      await this.loadLoginCaptcha()
      this.loginForm.captcha = ''
    },
    
    // 刷新注册验证码
    async refreshRegisterCaptcha() {
      await this.loadRegisterCaptcha()
      this.registerForm.captcha = ''
    },
    
    // 处理登录
    async handleLogin() {
      if (!this.isLoginFormValid) return
      
      this.loading = true
      
      try {
        const authStore = useAuthStore()
        const response = await authStore.login({
          username: this.loginForm.username,
          password: this.loginForm.password,
          captcha: this.loginForm.captcha,
          captchaKey: this.loginForm.captchaKey
        })
        
        if (response.success) {
          this.showToast('登录成功！', 'success')
          
          // 延迟跳转到主页
          setTimeout(() => {
            this.$router.push('/dashboard')
          }, 1000)
        } else {
          this.showToast(response.message || '登录失败', 'error')
          await this.refreshLoginCaptcha()
        }
      } catch (error) {
        console.error('登录失败:', error)
        this.showToast(error.message || '登录失败，请重试', 'error')
        await this.refreshLoginCaptcha()
      } finally {
        this.loading = false
      }
    },
    
    // 处理注册
    async handleRegister() {
      if (!this.isRegisterFormValid) return
      
      this.loading = true
      
      try {
        const authStore = useAuthStore()
        const response = await authStore.register({
          username: this.registerForm.username,
          nickName: this.registerForm.nickName || this.registerForm.username,
          password: this.registerForm.password,
          confirmPassword: this.registerForm.confirmPassword,
          captcha: this.registerForm.captcha,
          captchaKey: this.registerForm.captchaKey
        })
        
        if (response.success) {
          this.showToast('注册成功！请登录', 'success')
          
          // 清空注册表单，切换到登录页面
          this.registerForm = {
            username: '',
            nickName: '',
            password: '',
            confirmPassword: '',
            captcha: '',
            captchaKey: ''
          }
          this.errors = {}
          
          setTimeout(() => {
            this.activeTab = 'login'
          }, 1500)
        } else {
          this.showToast(response.message || '注册失败', 'error')
          await this.refreshRegisterCaptcha()
        }
      } catch (error) {
        console.error('注册失败:', error)
        this.showToast(error.message || '注册失败，请重试', 'error')
        await this.refreshRegisterCaptcha()
      } finally {
        this.loading = false
      }
    },
    
    // 表单验证方法
    validateUsername() {
      const username = this.registerForm.username.trim()
      if (!username) {
        delete this.errors.username
        return
      }
      
      const usernameRegex = /^[a-zA-Z0-9_]{4,16}$/
      if (!usernameRegex.test(username)) {
        this.errors.username = '用户名必须是4-16位字母数字下划线'
      } else {
        delete this.errors.username
      }
    },
    
    validatePassword() {
      const password = this.registerForm.password.trim()
      if (!password) {
        delete this.errors.password
        return
      }
      
      const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d@$!%*?&]{8,20}$/
      if (!passwordRegex.test(password)) {
        this.errors.password = '密码必须包含大小写字母和数字，长度8-20位'
      } else {
        delete this.errors.password
      }
      
      // 重新验证确认密码
      if (this.registerForm.confirmPassword) {
        this.validateConfirmPassword()
      }
    },
    
    validateConfirmPassword() {
      const confirmPassword = this.registerForm.confirmPassword.trim()
      if (!confirmPassword) {
        delete this.errors.confirmPassword
        return
      }
      
      if (confirmPassword !== this.registerForm.password) {
        this.errors.confirmPassword = '两次密码输入不一致'
      } else {
        delete this.errors.confirmPassword
      }
    },
    
    showToast(message, type = 'info') {
      this.toast = {
        show: true,
        message,
        type
      }
    },
    
    hideToast() {
      this.toast.show = false
    },
    
    // 获取验证码图片源
    getCaptchaImageSrc(captchaData) {
      console.log('getCaptchaImageSrc被调用，数据类型:', typeof captchaData)
      console.log('getCaptchaImageSrc被调用，数据长度:', captchaData?.length)
      console.log('getCaptchaImageSrc被调用，数据前50字符:', captchaData?.substring(0, 50))
      
      if (!captchaData) {
        console.log('验证码数据为空，返回空字符串')
        return ''
      }
      
      // 检查是否已经是完整的data URL
      if (captchaData.startsWith('data:image/')) {
        console.log('验证码数据已经是完整的data URL')
        return captchaData
      }
      
      // 如果不是，添加data URL前缀
      const result = `data:image/png;base64,${captchaData}`
      console.log('添加data URL前缀后的结果前100字符:', result.substring(0, 100))
      return result
    },
    
    // 验证码图片加载成功处理
    onCaptchaImageLoad(event) {
      console.log('验证码图片加载成功:', event.target.src.substring(0, 50) + '...')
    },
    
    // 验证码图片加载错误处理
    onCaptchaImageError(event) {
      console.error('验证码图片加载失败:', event)
      console.log('当前验证码数据长度:', this.loginCaptchaData.captcha?.length)
      console.log('验证码数据前100字符:', this.loginCaptchaData.captcha?.substring(0, 100))
      console.log('图片src:', event.target.src.substring(0, 100))
      this.showToast('验证码图片加载失败，请点击刷新', 'error')
    },
    
    // 测试验证码API
    async testCaptchaApi() {
      try {
        const response = await fetch('/api/auth/captcha')
        const data = await response.json()
        console.log('直接API测试结果:', data)
      } catch (error) {
        console.error('API测试失败:', error)
      }
    }
  }
}
</script>

<style scoped>
.auth-landing {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.auth-container {
  width: 100%;
  max-width: 450px;
  background-color: var(--white);
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  animation: slideUp 0.6s ease-out;
}

.auth-header {
  text-align: center;
  padding: 40px 40px 20px;
  background: linear-gradient(135deg, var(--primary-color) 0%, #3367d6 100%);
  color: var(--white);
}

.auth-title {
  font-size: 2.2rem;
  margin-bottom: 8px;
  font-weight: 600;
}

.auth-subtitle {
  font-size: 1rem;
  opacity: 0.9;
}

.auth-content {
  padding: 0;
}

.auth-tabs {
  display: flex;
  background-color: #f8f9fa;
}

.auth-tab {
  flex: 1;
  padding: 16px;
  border: none;
  background-color: transparent;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  color: #666;
}

.auth-tab.active {
  background-color: var(--white);
  color: var(--primary-color);
  border-bottom: 3px solid var(--primary-color);
}

.auth-tab:hover:not(.active) {
  background-color: #e9ecef;
}

.auth-form-container {
  padding: 30px 40px;
}

.test-account-tip {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  margin-bottom: 20px;
  background: linear-gradient(135deg, #e3f2fd 0%, #f3e5f5 100%);
  border: 1px solid #e1bee7;
  border-radius: 8px;
  animation: tipFadeIn 0.5s ease-out;
}

.tip-icon {
  font-size: 20px;
  margin-top: 2px;
}

.tip-content {
  flex: 1;
}

.tip-title {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--primary-color);
  margin: 0 0 4px 0;
}

.tip-text {
  font-size: 0.8rem;
  color: #666;
  margin: 0;
  line-height: 1.4;
}

@keyframes tipFadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-label {
  margin-bottom: 6px;
  color: var(--text-color);
  font-weight: 500;
  font-size: 0.9rem;
}

.form-input {
  padding: 12px 16px;
  border: 2px solid #e1e5e9;
  border-radius: 8px;
  font-size: 1rem;
  transition: all 0.3s ease;
  background-color: #fafbfc;
}

.form-input:focus {
  outline: none;
  border-color: var(--primary-color);
  background-color: var(--white);
  box-shadow: 0 0 0 3px rgba(66, 133, 244, 0.1);
}

.form-input:disabled {
  background-color: #f5f5f5;
  cursor: not-allowed;
}

.error-text {
  color: var(--quaternary-color);
  font-size: 0.8rem;
  margin-top: 4px;
}

.captcha-group {
  display: flex;
  gap: 12px;
  align-items: center;
}

.captcha-input {
  flex: 1;
}

.captcha-image {
  width: 120px;
  height: 44px;
  border: 2px solid #e1e5e9;
  border-radius: 8px;
  cursor: pointer;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #fafbfc;
  transition: border-color 0.3s ease;
}

.captcha-image:hover {
  border-color: var(--primary-color);
}

.captcha-image img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background-color: #fff;
  border-radius: 4px;
}

.captcha-loading {
  font-size: 0.8rem;
  color: #666;
}

.captcha-error {
  font-size: 0.8rem;
  color: var(--quaternary-color);
  text-align: center;
  cursor: pointer;
  padding: 8px;
  border: 1px dashed var(--quaternary-color);
  border-radius: 4px;
  background-color: #fef2f2;
  transition: all 0.3s ease;
}

.captcha-error:hover {
  background-color: #fecaca;
}

.auth-button {
  padding: 14px;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-top: 10px;
}

.login-button {
  background: linear-gradient(135deg, var(--primary-color) 0%, #3367d6 100%);
  color: var(--white);
}

.login-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(66, 133, 244, 0.3);
}

.register-button {
  background: linear-gradient(135deg, var(--secondary-color) 0%, #2d9247 100%);
  color: var(--white);
}

.register-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(52, 168, 83, 0.3);
}

.auth-button:disabled {
  background: #ccc;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.auth-footer {
  padding: 20px;
  text-align: center;
  background-color: #f8f9fa;
  border-top: 1px solid #e9ecef;
}

.auth-footer p {
  color: #666;
  font-size: 0.8rem;
  margin: 0;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式调整 */
@media (max-width: 480px) {
  .auth-container {
    margin: 10px;
    max-width: none;
  }
  
  .auth-header {
    padding: 30px 20px 15px;
  }
  
  .auth-title {
    font-size: 1.8rem;
  }
  
  .auth-form-container {
    padding: 20px;
  }
  
  .captcha-group {
    flex-direction: column;
    align-items: stretch;
  }
  
  .captcha-image {
    width: 100%;
    height: 50px;
  }
  
  .test-account-tip {
    padding: 12px;
    margin-bottom: 15px;
  }
  
  .tip-icon {
    font-size: 18px;
  }
  
  .tip-title {
    font-size: 0.85rem;
  }
  
  .tip-text {
    font-size: 0.75rem;
  }
}
</style>