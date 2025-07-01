<template>
  <div class="chat-container">
    <header class="chat-header">
      <div class="container">
        <div class="header-content">
          <router-link to="/" class="back-button">
            <span class="back-icon">←</span> 返回
          </router-link>
          <h1 class="chat-title">疾控监督专家</h1>
          <div class="chat-id">会话ID: {{ chatId }}</div>
        </div>
      </div>
    </header>
    
    <main class="chat-main">
      <div class="container chat-container-inner">
        <div class="sidebar-wrapper" :class="{ 'sidebar-collapsed': isSidebarCollapsed }">
          <div class="chat-sidebar">
            <div class="sidebar-collapse-btn" @click="toggleSidebar" :title="isSidebarCollapsed ? '展开侧边栏' : '收起侧边栏'">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.5">
                <rect x="4" y="4" width="16" height="16" rx="3" />
                <path d="M14 8L10 12L14 16" />
              </svg>
            </div>
            <div class="sidebar-header">
              <h3 class="sidebar-title">历史会话</h3>
            </div>
            
            <button class="new-chat-button" @click="createNewChat">
              <span class="icon">+</span>
              新建对话
            </button>
            
            <div class="session-list">
              <div 
                v-for="(session, id) in chatSessions" 
                :key="id" 
                class="session-item"
                :class="{ active: id === chatId }"
              >
                <div class="session-content" @click="switchSession(id)">
                  <span class="session-name" :title="getSessionTitle(session)">{{ getSessionTitle(session) }}</span>
                  <span class="session-time">{{ formatSessionTime(session) }}</span>
                </div>
                <button class="delete-session-btn" @click.stop="confirmDeleteSession(id)" title="删除会话">
                  <span class="delete-icon">×</span>
                </button>
              </div>
              <div v-if="Object.keys(chatSessions).length === 0" class="no-sessions">
                暂无历史会话
              </div>
            </div>
          </div>
        </div>
        
        <div class="chat-content" :class="{ 'content-expanded': isSidebarCollapsed }">
          <!-- 侧边栏收起后的展开按钮 -->
          <div v-if="isSidebarCollapsed" class="sidebar-expand-btn" @click="toggleSidebar" title="展开侧边栏">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.5">
              <rect x="4" y="4" width="16" height="16" rx="3" />
              <path d="M10 8L14 12L10 16" />
            </svg>
            <span class="tooltip">展开侧边栏</span>
          </div>
          
          <div class="chat-messages" ref="messagesContainer">
            <div v-if="messages.length === 0" class="empty-chat">
              <div class="welcome-message">
                <img :src="aiAvatar" alt="疾控监督专家" class="welcome-avatar" />
                <h2>欢迎使用疾控监督专家</h2>
                <p>请输入您的问题，我将尽力为您提供专业解答。</p>
              </div>
            </div>
            
            <chat-message
              v-for="(message, index) in messages"
              :key="index"
              :role="message.role"
              :content="message.content"
              :timestamp="message.timestamp"
              ai-name="疾控监督专家"
              :ai-avatar="aiAvatar"
            />
          </div>
          
          <div class="chat-input-area">
            <div class="input-container">
              <textarea
                v-model="userInput"
                class="message-input"
                placeholder="请输入您的问题..."
                @keydown.enter.prevent="sendMessage"
                :disabled="isLoading"
              ></textarea>
              <button @click="sendMessage" class="send-button" :disabled="isLoading || !userInput.trim()">
                {{ isLoading ? '发送中...' : '发送' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </main>
    
    <!-- 删除确认对话框 -->
    <div v-if="showDeleteConfirm" class="delete-confirm-modal">
      <div class="delete-confirm-content">
        <h3>确认删除</h3>
        <p>确定要删除这个会话吗？此操作不可恢复。</p>
        <div class="delete-confirm-buttons">
          <button class="cancel-btn" @click="cancelDelete">取消</button>
          <button class="confirm-btn" @click="deleteSession">删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { useChatStore } from '../store/chat';
import ChatMessage from '../components/ChatMessage.vue';
import api from '../utils/api';
import { generateChatId, formatDateTime } from '../utils/helpers';
import aiAvatarInspector from '../assets/ai-avatar-inspector.svg';

export default {
  name: 'InspectorApp',
  components: {
    ChatMessage
  },
  data() {
    return {
      chatId: '',
      userInput: '',
      currentResponse: '',
      isLoading: false,
      sseConnection: null,
      aiAvatar: aiAvatarInspector,
      showDeleteConfirm: false,
      sessionToDelete: null,
      isSidebarCollapsed: false
    };
  },
  computed: {
    messages() {
      return this.chatStore.getInspectorChat(this.chatId);
    },
    chatSessions() {
      return this.chatStore.inspectorChats;
    }
  },
  created() {
    this.chatStore = useChatStore();
    
    // 从路由参数获取聊天ID，如果没有则生成一个新的
    this.chatId = this.$route.params.chatId || generateChatId();
    
    // 如果是新的URL但没有chatId参数，更新URL
    if (!this.$route.params.chatId) {
      this.$router.replace({ name: 'inspector-app', params: { chatId: this.chatId } });
    }
    
    // 从本地存储加载侧边栏状态
    const savedState = localStorage.getItem('inspectorSidebarCollapsed');
    if (savedState !== null) {
      this.isSidebarCollapsed = savedState === 'true';
    }
  },
  mounted() {
    this.scrollToBottom();
  },
  updated() {
    this.scrollToBottom();
  },
  methods: {
    sendMessage() {
      if (!this.userInput.trim() || this.isLoading) return;
      
      const message = this.userInput;
      this.userInput = '';
      
      // 添加用户消息到聊天记录
      this.chatStore.addInspectorUserMessage(this.chatId, message);
      
      // 开始加载状态
      this.isLoading = true;
      this.currentResponse = '';
      
      // 创建占位回复
      this.chatStore.addInspectorAiMessage(this.chatId, this.currentResponse);
      
      // 发送SSE请求
      this.sseConnection = api.chatWithInspectorSSE(
        message,
        this.chatId,
        this.handleSSEMessage,
        this.handleSSEError,
        this.handleSSEComplete
      );
    },
    
    handleSSEMessage(data) {
      // 更新当前响应内容
      this.currentResponse += data;
      
      // 更新最后一条AI消息
      const chatMessages = this.chatStore.getInspectorChat(this.chatId);
      const lastMessage = chatMessages[chatMessages.length - 1];
      
      if (lastMessage && lastMessage.role === 'assistant') {
        lastMessage.content = this.currentResponse;
      }
      
      // 确保每次收到消息时都滚动到底部
      this.scrollToBottom();
    },
    
    handleSSEError(error) {
      console.error('SSE连接错误:', error);
      this.isLoading = false;
      
      // 如果没有收到任何响应，添加错误消息
      if (!this.currentResponse) {
        this.chatStore.addInspectorAiMessage(
          this.chatId, 
          '很抱歉，连接出现问题，请稍后再试。'
        );
      }
    },
    
    handleSSEComplete() {
      this.isLoading = false;
      this.sseConnection = null;
    },
    
    scrollToBottom() {
      if (this.$refs.messagesContainer) {
        setTimeout(() => {
          const container = this.$refs.messagesContainer;
          container.scrollTop = container.scrollHeight;
        }, 10);
      }
    },
    
    switchSession(sessionId) {
      // 如果正在加载，先中断当前请求
      if (this.isLoading && this.sseConnection) {
        this.sseConnection.close();
        this.isLoading = false;
      }
      
      // 切换到选定的会话
      this.$router.replace({ name: 'inspector-app', params: { chatId: sessionId } });
      this.chatId = sessionId;
      this.$nextTick(() => {
        this.scrollToBottom();
      });
    },
    
    createNewChat() {
      // 如果正在加载，先中断当前请求
      if (this.isLoading && this.sseConnection) {
        this.sseConnection.close();
        this.isLoading = false;
      }
      
      // 生成新的聊天ID
      const newChatId = generateChatId();
      
      // 切换到新会话
      this.$router.replace({ name: 'inspector-app', params: { chatId: newChatId } });
      this.chatId = newChatId;
      this.userInput = '';
    },
    
    confirmDeleteSession(sessionId) {
      this.sessionToDelete = sessionId;
      this.showDeleteConfirm = true;
    },
    
    cancelDelete() {
      this.showDeleteConfirm = false;
      this.sessionToDelete = null;
    },
    
    deleteSession() {
      if (!this.sessionToDelete) return;
      
      // 删除会话
      this.chatStore.deleteInspectorChat(this.sessionToDelete);
      
      // 如果删除的是当前会话，则创建一个新会话
      if (this.sessionToDelete === this.chatId) {
        this.createNewChat();
      }
      
      // 关闭确认对话框
      this.showDeleteConfirm = false;
      this.sessionToDelete = null;
    },
    
    formatSessionTime(session) {
      if (!session || session.length === 0) return '';
      
      // 获取最后一条消息的时间
      const lastMessage = session[session.length - 1];
      if (lastMessage && lastMessage.timestamp) {
        const date = new Date(lastMessage.timestamp);
        return date.toLocaleDateString('zh-CN');
      }
      return '';
    },
    
    getSessionTitle(session) {
      if (!session || session.length === 0) return '新会话';
      
      // 获取第一条用户消息作为标题
      const firstUserMessage = session.find(msg => msg.role === 'user');
      if (firstUserMessage && firstUserMessage.content) {
        // 截取适当长度作为标题
        const title = firstUserMessage.content.trim();
        return title.length > 20 ? title.substring(0, 20) + '...' : title;
      }
      return '新会话';
    },
    
    toggleSidebar() {
      this.isSidebarCollapsed = !this.isSidebarCollapsed;
      // 保存用户偏好到本地存储
      localStorage.setItem('inspectorSidebarCollapsed', this.isSidebarCollapsed);
    }
  },
  beforeUnmount() {
    // 确保关闭SSE连接
    if (this.sseConnection) {
      this.sseConnection.close();
    }
  }
}
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  position: relative;
}

.chat-header {
  background-color: var(--white);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  padding: 15px 0;
  position: sticky;
  top: 0;
  z-index: 10;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.back-button {
  display: flex;
  align-items: center;
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 500;
}

.back-icon {
  margin-right: 5px;
  font-size: 1.2rem;
}

.chat-title {
  font-size: 1.5rem;
  margin: 0;
  color: var(--text-color);
}

.chat-id {
  font-size: 0.85rem;
  color: #666;
}

.chat-main {
  flex: 1;
  overflow: hidden;
  padding: 20px 0;
  background-color: var(--background-color);
}

.chat-container-inner {
  display: flex;
  height: 100%;
}

.sidebar-wrapper {
  width: 280px;
  min-width: 280px;
  position: relative;
  transition: all 0.3s ease;
  margin-right: 20px;
}

.sidebar-collapsed {
  width: 0 !important;
  min-width: 0 !important;
  margin-right: 0 !important;
}

.chat-sidebar {
  width: 100%;
  height: 100%;
  background-color: var(--white);
  border-right: 1px solid var(--light-gray);
  border-radius: 8px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  position: relative;
  box-shadow: var(--shadow);
}

.sidebar-collapsed .chat-sidebar {
  opacity: 0;
  pointer-events: none;
}

.sidebar-collapse-btn {
  position: absolute;
  top: 20px;
  right: -16px;
  width: 32px;
  height: 32px;
  background-color: var(--white);
  border: 1px solid var(--light-gray);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 100;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
  color: #666;
}

.sidebar-collapse-btn:hover {
  background-color: #f0f5ff;
  color: var(--primary-color);
  transform: translateX(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.sidebar-collapse-btn::after {
  content: "收起侧边栏";
  position: absolute;
  top: 50%;
  right: 42px;
  transform: translateY(-50%);
  background-color: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
  opacity: 0;
  visibility: hidden;
  transition: all 0.2s ease;
  pointer-events: none;
}

.sidebar-collapse-btn:hover::after {
  opacity: 1;
  visibility: visible;
}

.sidebar-collapsed .sidebar-collapse-btn {
  opacity: 0;
  visibility: hidden;
}

/* 侧边栏展开按钮 */
.sidebar-expand-btn {
  position: absolute;
  top: 20px;
  left: -16px;
  width: 32px;
  height: 32px;
  background-color: var(--white);
  border: 1px solid var(--light-gray);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 100;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
  color: #666;
}

.sidebar-expand-btn:hover {
  background-color: #f0f5ff;
  color: var(--primary-color);
  transform: translateX(2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.tooltip {
  position: absolute;
  left: 42px;
  background-color: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
  opacity: 0;
  visibility: hidden;
  transition: all 0.2s ease;
  pointer-events: none;
}

.sidebar-expand-btn:hover .tooltip {
  opacity: 1;
  visibility: visible;
}

.sidebar-collapse-btn svg, .sidebar-expand-btn svg {
  stroke: #666;
  transition: stroke 0.2s ease;
}

.sidebar-collapse-btn:hover svg, .sidebar-expand-btn:hover svg {
  stroke: var(--primary-color);
}

.chat-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: var(--white);
  border-radius: 8px;
  box-shadow: var(--shadow);
  transition: all 0.3s ease;
  position: relative;
}

.content-expanded {
  max-width: 100%;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--light-gray);
}

.sidebar-title {
  margin: 0;
  font-size: 1.1rem;
  color: var(--text-color);
}

.new-chat-button {
  width: 100%;
  padding: 10px;
  border-radius: 6px;
  background-color: white;
  color: var(--text-color);
  border: 1px solid var(--light-gray);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  margin: 0 0 15px 0;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.new-chat-button:hover {
  background-color: rgba(0, 0, 0, 0.03);
  border-color: rgba(0, 0, 0, 0.1);
}

.icon {
  margin-right: 8px;
  font-size: 16px;
}

.session-list {
  flex: 1;
  overflow-y: auto;
}

.session-item {
  padding: 10px;
  border-radius: 6px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.session-item:hover {
  background-color: #f0f5ff;
}

.session-item.active {
  background-color: #e8f0fe;
  border-left: 3px solid var(--primary-color);
}

.session-content {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.session-name {
  font-weight: 500;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-time {
  font-size: 0.8rem;
  color: #777;
}

.delete-session-btn {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background-color: transparent;
  color: #999;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  margin-left: 8px;
  visibility: hidden;
  opacity: 0;
}

.session-item:hover .delete-session-btn {
  visibility: visible;
  opacity: 1;
}

.delete-session-btn:hover {
  background-color: #ff4d4f;
  color: white;
}

.delete-icon {
  font-size: 16px;
  font-weight: bold;
}

.no-sessions {
  color: #999;
  text-align: center;
  padding: 20px 0;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.empty-chat {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.welcome-message {
  text-align: center;
  padding: 30px;
  max-width: 500px;
  background-color: var(--white);
  border-radius: 12px;
  box-shadow: var(--shadow);
}

.welcome-avatar {
  width: 80px;
  height: 80px;
  margin-bottom: 20px;
}

.chat-input-area {
  padding: 15px;
  border-top: 1px solid var(--light-gray);
}

.input-container {
  display: flex;
  gap: 10px;
}

.message-input {
  flex: 1;
  padding: 12px 15px;
  border: 1px solid var(--light-gray);
  border-radius: 8px;
  resize: none;
  height: 60px;
  font-family: "Microsoft YaHei", sans-serif;
  font-size: 1rem;
}

.message-input:focus {
  outline: none;
  border-color: var(--primary-color);
}

.send-button {
  background-color: var(--primary-color);
  color: white;
  border: none;
  border-radius: 8px;
  padding: 0 20px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.send-button:hover:not(:disabled) {
  background-color: #3367d6;
}

.send-button:disabled {
  background-color: #a8c7fa;
  cursor: not-allowed;
}

/* 删除确认对话框样式 */
.delete-confirm-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.delete-confirm-content {
  background-color: white;
  border-radius: 8px;
  padding: 20px;
  width: 300px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.delete-confirm-content h3 {
  margin-top: 0;
  color: #ff4d4f;
}

.delete-confirm-buttons {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  gap: 10px;
}

.cancel-btn {
  padding: 8px 16px;
  border: 1px solid var(--light-gray);
  background-color: white;
  border-radius: 4px;
  cursor: pointer;
}

.confirm-btn {
  padding: 8px 16px;
  border: none;
  background-color: #ff4d4f;
  color: white;
  border-radius: 4px;
  cursor: pointer;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .chat-container-inner {
    flex-direction: column;
  }
  
  .sidebar-wrapper {
    width: 100%;
    margin-right: 0;
    margin-bottom: 15px;
    max-height: 200px;
  }
  
  .chat-header {
    padding: 10px 0;
  }
  
  .chat-title {
    font-size: 1.2rem;
  }
  
  .chat-id {
    display: none;
  }
  
  .input-container {
    flex-direction: column;
  }
  
  .send-button {
    height: 45px;
  }
  
  .delete-session-btn {
    visibility: visible;
    opacity: 1;
  }
}
</style> 