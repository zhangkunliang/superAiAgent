<template>
  <div class="chat-container">
    <header class="chat-header">
      <div class="container">
        <div class="header-content">
          <router-link to="/" class="back-button">
            <span class="back-icon">←</span> 返回
          </router-link>
          <h1 class="chat-title">超级智能体</h1>
          <div class="chat-id">会话ID: {{ chatId }}</div>
        </div>
      </div>
    </header>
    
    <main class="chat-main">
      <div class="container chat-container-inner">
        <div class="chat-sidebar">
          <div class="sidebar-header">
            <h3 class="sidebar-title">历史会话</h3>
            <button class="new-chat-btn" @click="createNewChat" title="新建会话">
              <span class="new-chat-icon">+</span>
            </button>
          </div>
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
        
        <div class="chat-content">
          <div class="chat-messages" ref="messagesContainer">
            <div v-if="messages.length === 0" class="empty-chat">
              <div class="welcome-message">
                <img :src="aiAvatar" alt="超级智能体" class="welcome-avatar" />
                <h2>欢迎使用超级智能体</h2>
                <p>我是您的多功能AI助手，可以执行复杂任务，提供全面的问题解决方案。请输入您的问题或指令。</p>
              </div>
            </div>
            
            <chat-message
              v-for="(message, index) in messages"
              :key="index"
              :role="message.role"
              :content="message.content"
              :timestamp="message.timestamp"
              ai-name="超级智能体"
              :ai-avatar="aiAvatar"
            />
          </div>
          
          <div class="chat-input-area">
            <div class="input-container">
              <textarea
                v-model="userInput"
                class="message-input"
                placeholder="请输入您的问题或指令..."
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
import aiAvatarManus from '../assets/ai-avatar-manus.svg';

export default {
  name: 'ManusApp',
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
      aiAvatar: aiAvatarManus,
      showDeleteConfirm: false,
      sessionToDelete: null
    };
  },
  computed: {
    messages() {
      return this.chatStore.getManusChat(this.chatId);
    },
    chatSessions() {
      return this.chatStore.manusChats;
    }
  },
  created() {
    this.chatStore = useChatStore();
    
    // 从路由参数获取聊天ID，如果没有则生成一个新的
    this.chatId = this.$route.params.chatId || generateChatId();
    
    // 如果是新的URL但没有chatId参数，更新URL
    if (!this.$route.params.chatId) {
      this.$router.replace({ name: 'manus-app', params: { chatId: this.chatId } });
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
      this.chatStore.addManusUserMessage(this.chatId, message);
      
      // 开始加载状态
      this.isLoading = true;
      this.currentResponse = '';
      
      // 创建占位回复
      this.chatStore.addManusAiMessage(this.chatId, this.currentResponse);
      
      // 发送SSE请求
      this.sseConnection = api.chatWithManusSSE(
        message,
        this.handleSSEMessage,
        this.handleSSEError,
        this.handleSSEComplete
      );
    },
    
    handleSSEMessage(data) {
      // 更新当前响应内容 - 对于Manus添加额外的换行
      this.currentResponse += data + '\n';
      
      // 更新最后一条AI消息
      const chatMessages = this.chatStore.getManusChat(this.chatId);
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
        this.chatStore.addManusAiMessage(
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
      this.$router.replace({ name: 'manus-app', params: { chatId: sessionId } });
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
      this.$router.replace({ name: 'manus-app', params: { chatId: newChatId } });
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
      this.chatStore.deleteManusChat(this.sessionToDelete);
      
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
  color: var(--secondary-color);
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

.chat-sidebar {
  width: 250px;
  background-color: var(--white);
  border-radius: 8px;
  padding: 15px;
  margin-right: 20px;
  box-shadow: var(--shadow);
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
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

.new-chat-btn {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background-color: var(--secondary-color);
  color: white;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color 0.2s;
}

.new-chat-btn:hover {
  background-color: #2d9247;
}

.new-chat-icon {
  font-size: 18px;
  font-weight: bold;
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
  border-left: 3px solid var(--secondary-color);
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

.chat-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: var(--white);
  border-radius: 8px;
  box-shadow: var(--shadow);
  overflow: hidden;
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
  border-color: var(--secondary-color);
}

.send-button {
  background-color: var(--secondary-color);
  color: white;
  border: none;
  border-radius: 8px;
  padding: 0 20px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.send-button:hover:not(:disabled) {
  background-color: #2d9247;
}

.send-button:disabled {
  background-color: #a7d7b8;
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
  
  .chat-sidebar {
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