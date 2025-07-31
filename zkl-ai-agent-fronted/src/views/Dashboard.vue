<template>
  <div class="dashboard">
    <header class="header">
      <div class="container">
        <div class="header-top">
          <div class="header-content">
            <h1 class="title">疾控监督AI 智能体平台</h1>
            <p class="subtitle">选择您需要使用的疾控监督 AI 助手</p>
          </div>
          <div class="header-actions">
            <div class="user-info">
              <span class="welcome-text">欢迎，{{ authStore.user?.nickName || authStore.user?.username }}</span>
              <button @click="handleLogout" class="logout-btn">退出登录</button>
            </div>
          </div>
        </div>
      </div>
    </header>
    
    <main class="main">
      <div class="container">
        <div class="app-grid">
          <router-link :to="{ name: 'inspector-app', params: { chatId: generateNewChatId() } }" class="app-card">
            <div class="app-icon">
              <img :src="inspectorAvatar" alt="疾控监督专家" />
            </div>
            <h2 class="app-title">疾控监督专家</h2>
            <p class="app-description">参照医疗卫生法律法规知识库，专业解答疾控监督相关问题，提供政策咨询及实务指导。</p>
          </router-link>
          
          <router-link :to="{ name: 'manus-app', params: { chatId: generateNewChatId() } }" class="app-card">
            <div class="app-icon">
              <img :src="manusAvatar" alt="超级智能体" />
            </div>
            <h2 class="app-title">超级智能体</h2>
            <p class="app-description">多功能 AI 助手，可执行复杂任务，提供全面的问题解决方案，包括疾控监督、疫情防控、卫生法律法规等。</p>
          </router-link>
        </div>
      </div>
    </main>
  </div>
</template>

<script>
import { generateChatId } from '../utils/helpers';
import { useAuthStore } from '../store/auth';
import inspectorAvatar from '../assets/ai-avatar-inspector.svg';
import manusAvatar from '../assets/ai-avatar-manus.svg';

export default {
  name: 'Dashboard',
  data() {
    return {
      inspectorAvatar,
      manusAvatar,
      authStore: useAuthStore()
    };
  },
  async mounted() {
    // 初始化认证状态
    if (this.authStore.token && !this.authStore.user) {
      try {
        await this.authStore.initAuth();
      } catch (error) {
        console.error('初始化认证状态失败:', error);
        // 如果初始化失败，跳转到登录页
        this.$router.push('/');
      }
    }
  },
  methods: {
    generateNewChatId() {
      return generateChatId();
    },
    async handleLogout() {
      try {
        await this.authStore.logout();
        this.$router.push('/');
      } catch (error) {
        console.error('退出登录失败:', error);
      }
    }
  }
}
</script>

<style scoped>
.dashboard {
  min-height: 100vh;
  background-color: var(--background-color);
}

.header {
  padding: 60px 0 40px;
  background-color: var(--white);
  box-shadow: var(--shadow);
  margin-bottom: 40px;
}

.header-top {
  position: relative;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
}

.header-content {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  white-space: nowrap;
}

.header-actions {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.welcome-text {
  color: var(--text-color);
  font-weight: 500;
}

.logout-btn {
  padding: 8px 16px;
  background-color: var(--quaternary-color);
  color: var(--white);
  border: none;
  border-radius: 6px;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.3s ease;
}

.logout-btn:hover {
  background-color: #d73527;
  transform: translateY(-1px);
}

.title {
  font-size: 2.5rem;
  margin-bottom: 1rem;
  color: var(--primary-color);
}

.subtitle {
  font-size: 1.2rem;
  color: #666;
  max-width: 600px;
  margin: 0 auto;
}

.main {
  padding-bottom: 60px;
}

.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 30px;
  max-width: 900px;
  margin: 0 auto;
}

.app-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30px;
  border-radius: 10px;
  background-color: var(--white);
  box-shadow: var(--shadow);
  transition: transform 0.3s, box-shadow 0.3s;
  text-decoration: none;
  color: var(--text-color);
}

.app-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.1);
}

.app-icon {
  width: 80px;
  height: 80px;
  margin-bottom: 20px;
}

.app-icon img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.app-title {
  font-size: 1.5rem;
  margin-bottom: 15px;
  color: var(--text-color);
}

.app-description {
  text-align: center;
  color: #666;
  line-height: 1.5;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .header {
    padding: 40px 0 30px;
  }
  
  .header-top {
    position: static;
    flex-direction: column;
    justify-content: center;
    text-align: center;
  }
  
  .header-content {
    position: static;
    transform: none;
    order: 1;
  }
  
  .header-actions {
    order: 2;
  }
  
  .title {
    font-size: 2rem;
  }
  
  .app-grid {
    grid-template-columns: 1fr;
  }
  
  .user-info {
    flex-direction: column;
    gap: 10px;
  }
}
</style>