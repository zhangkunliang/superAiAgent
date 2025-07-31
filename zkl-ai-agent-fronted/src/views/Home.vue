<template>
  <div class="home-redirect">
    <div class="loading-container">
      <div class="loading-spinner"></div>
      <p class="loading-text">正在跳转...</p>
    </div>
  </div>
</template>

<script>
import { useAuthStore } from '../store/auth';

export default {
  name: 'HomeRedirect',
  async mounted() {
    const authStore = useAuthStore();
    
    // 检查认证状态并跳转
    if (authStore.isAuthenticated) {
      this.$router.replace('/dashboard');
    } else {
      this.$router.replace('/');
    }
  }
}
</script>

<style scoped>
.home-redirect {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.loading-container {
  text-align: center;
  color: var(--white);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid rgba(255, 255, 255, 0.3);
  border-top: 4px solid var(--white);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

.loading-text {
  font-size: 1.1rem;
  font-weight: 500;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style> 