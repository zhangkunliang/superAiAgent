<template>
  <div class="home">
    <header class="header">
      <div class="container">
        <h1 class="title"> AI 智能体平台</h1>
        <p class="subtitle">选择您需要使用的 AI 助手</p>
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

          <router-link :to="{ name: 'test' }" class="app-card test-card">
            <div class="app-icon">
              <div class="test-icon">🧪</div>
            </div>
            <h2 class="app-title">换行符测试</h2>
            <p class="app-description">测试后台返回内容中换行符的显示效果。</p>
          </router-link>
        </div>
      </div>
    </main>
  </div>
</template>

<script>
import { generateChatId } from '../utils/helpers';
import inspectorAvatar from '../assets/ai-avatar-inspector.svg';
import manusAvatar from '../assets/ai-avatar-manus.svg';

export default {
  name: 'HomeView',
  data() {
    return {
      inspectorAvatar,
      manusAvatar
    };
  },
  methods: {
    generateNewChatId() {
      return generateChatId();
    }
  }
}
</script>

<style scoped>
.home {
  min-height: calc(100vh - 50px); /* 减去footer高度 */
}

.header {
  padding: 60px 0 40px;
  text-align: center;
  background-color: var(--white);
  box-shadow: var(--shadow);
  margin-bottom: 40px;
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

.test-card {
  border: 2px dashed #ddd;
  background-color: #f9f9f9;
}

.test-icon {
  font-size: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .header {
    padding: 40px 0 30px;
  }
  
  .title {
    font-size: 2rem;
  }
  
  .app-grid {
    grid-template-columns: 1fr;
  }
}
</style> 