<template>
  <div class="chat-container">
    <header class="chat-header">
      <div class="container">
        <div class="header-content">
          <router-link to="/" class="back-button">
            <span class="back-icon">←</span> 返回
          </router-link>
          <h1 class="chat-title">疾控监督专家</h1>
          <div class="header-right">
            <div class="export-session-buttons" v-if="messages.length > 0">
              <button
                @click="exportSessionToWord"
                class="export-session-btn export-word"
                title="导出整个会话为Word文档"
              >
                📄 导出Word
              </button>
              <button
                @click="exportSessionToPDF"
                class="export-session-btn export-pdf"
                title="导出整个会话为PDF文档"
              >
                📋 导出PDF
              </button>
            </div>
            <div class="chat-id">会话ID: {{ chatId }}</div>
          </div>
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
                <p>专业的疾控监督AI助手，为您提供权威的疾病预防控制指导。</p>
                
                <!-- 功能亮点展示 -->
                <div class="feature-highlights">
                  <div class="feature-item">
                    <span class="feature-icon">🏥</span>
                    <span class="feature-text">疾病预防咨询</span>
                  </div>
                  <div class="feature-item">
                    <span class="feature-icon">📋</span>
                    <span class="feature-text">监督检查指导</span>
                  </div>
                  <div class="feature-item">
                    <span class="feature-icon">⚡</span>
                    <span class="feature-text">实时智能解答</span>
                  </div>
                  <div class="feature-item">
                    <span class="feature-icon">📄</span>
                    <span class="feature-text">文档导出功能</span>
                  </div>
                </div>
                
                <div class="quick-tips">
                  <p class="tips-title">💡 您可以询问：</p>
                  <ul class="tips-list">
                    <li>疾病预防控制相关问题</li>
                    <li>卫生监督检查标准</li>
                    <li>公共卫生应急处理</li>
                    <li>法规政策解读</li>
                  </ul>
                </div>
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
              @export-success="handleExportSuccess"
              @export-error="handleExportError"
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
import { exportToWord, exportToPDF } from '../utils/exportUtils';
import { exportToHTML, exportToText, exportToMarkdown } from '../utils/simpleExport';

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
      isSidebarCollapsed: false,
      
      // 新增：消息处理优化相关
      messageBuffer: [], // 消息缓冲区
      updateTimer: null, // 更新定时器
      autoScroll: true, // 自动滚动开关
      
      // 新增：错误处理相关
      connectionRetries: 0,
      maxRetries: 3,
      retryDelay: 1000,
      isReceivingData: false, // 标记是否正在接收数据
      hasReceivedData: false, // 标记是否已收到数据
      
      // 新增：性能监控
      lastMessageTime: 0,
      messageCount: 0,
      averageResponseTime: 0
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
  beforeUnmount() {
    // 组件销毁前清理资源
    this.cleanupResources();
  },
  methods: {
    sendMessage() {
      if (!this.userInput.trim() || this.isLoading) return;
      
      const message = this.userInput;
      this.userInput = '';
      
      // 重置消息状态
      this.resetMessageState();
      
      // 记录发送时间（用于性能监控）
      this.lastMessageTime = Date.now();
      
      // 添加用户消息到聊天记录
      this.chatStore.addInspectorUserMessage(this.chatId, message);
      
      // 开始加载状态和初始化
      this.isLoading = true;
      this.currentResponse = '🤔 疾控监督专家正在思考中...';
      
      // 创建初始占位回复
      this.chatStore.addInspectorAiMessage(this.chatId, this.currentResponse);
      
      // 发送请求
      try {
        this.sseConnection = api.chatWithInspectorSSE(
          message,
          this.chatId,
          this.handleSSEMessage,
          this.handleSSEError,
          this.handleSSEComplete
        );
      } catch (error) {
        console.error('发送消息失败:', error);
        this.handleSSEError(error);
      }
    },
    
    resetMessageState() {
      // 重置消息相关状态
      this.currentResponse = '';
      this.messageBuffer = [];
      this.connectionRetries = 0;
      this.isReceivingData = false;
      this.hasReceivedData = false;
      
      // 清理现有定时器
      if (this.updateTimer) {
        clearTimeout(this.updateTimer);
        this.updateTimer = null;
      }
    },
    
    handleSSEMessage(data) {
      try {
        // 标记正在接收数据
        this.isReceivingData = true;
        this.hasReceivedData = true;
        
        // 调试：记录接收数据（开发环境）
        if (process.env.NODE_ENV === 'development') {
          console.log('Inspector SSE received:', data.substring(0, 100) + '...');
        }

        // 尝试解析JSON格式的消息（支持结构化消息）
        let messageData;
        try {
          messageData = JSON.parse(data);
          this.handleStructuredMessage(messageData);
          return;
        } catch (e) {
          // 如果不是JSON格式，按原文本处理
          console.log('Non-JSON message, treating as plain text');
        }

        // 优化：批量更新减少重绘
        this.messageBuffer.push(data);
        
        // 节流处理：避免过于频繁的DOM更新
        if (!this.updateTimer) {
          this.updateTimer = setTimeout(() => {
            this.flushMessageBuffer();
            this.updateTimer = null;
          }, 50); // 50ms节流间隔
        }

      } catch (error) {
        console.error('Error handling SSE message:', error);
        // 降级处理：直接追加文本，但不触发重连
        this.appendToResponse(data);
      }
    },
    
    handleStructuredMessage(messageData) {
      const { type, content, finished } = messageData;
      
      // 根据消息类型处理（为未来扩展预留）
      switch (type) {
        case 'THINKING':
          this.appendToResponse(`💭 **思考中**\n${content}\n\n`);
          break;
          
        case 'CONTENT':
          this.appendToResponse(content);
          break;
          
        case 'COMPLETE':
          this.appendToResponse(content);
          this.markAsComplete();
          break;
          
        case 'ERROR':
          this.appendToResponse(`❌ **错误**\n${content}\n\n`);
          this.markAsError();
          break;
          
        default:
          // 未知类型，按内容处理
          this.appendToResponse(content);
      }

      // 智能滚动
      this.smartScrollToBottom();
    },
    
    flushMessageBuffer() {
      if (this.messageBuffer.length === 0) return;
      
      // 批量处理缓冲区的消息
      const batchData = this.messageBuffer.join('');
      this.messageBuffer = [];
      
      // 追加到响应中
      this.appendToResponse(batchData);
      
      // 智能滚动：只在用户接近底部时自动滚动
      this.smartScrollToBottom();
    },
    
    appendToResponse(content) {
      if (content && content.trim()) {
        // 如果是第一次接收到真实内容，清除占位符
        if (this.currentResponse.includes('🤔 疾控监督专家正在思考中...')) {
          this.currentResponse = '';
        }
        
        this.currentResponse += content;
        this.updateLastMessage();
      }
    },
    
    updateLastMessage() {
      const chatMessages = this.chatStore.getInspectorChat(this.chatId);
      const lastMessage = chatMessages[chatMessages.length - 1];
      
      if (lastMessage && lastMessage.role === 'assistant') {
        lastMessage.content = this.currentResponse;
      }
    },
    
    markAsComplete() {
      // 标记任务完成
      this.isLoading = false;
    },
    
    markAsError() {
      // 标记任务出错
      this.isLoading = false;
    },
    
    handleSSEError(error) {
      console.error('SSE连接错误:', error);
      
      // 重要：如果已经在接收数据过程中，或者已经收到了部分数据，不应该重试
      // 这通常表示连接正常，只是数据传输过程中的临时问题
      if (this.isReceivingData || this.hasReceivedData) {
        console.log('正在传输数据过程中出现错误，不触发重连机制');
        this.isLoading = false;
        
        // 如果有部分响应内容，保留它们，只添加错误提示
        if (this.currentResponse && !this.currentResponse.includes('🤔 疾控监督专家正在思考中...')) {
          this.appendToResponse('\n\n⚠️ 传输完成，如有遗漏请重新提问。');
        } else {
          this.showErrorMessage(error);
        }
        return;
      }
      
      // 清理定时器和缓冲区
      if (this.updateTimer) {
        clearTimeout(this.updateTimer);
        this.updateTimer = null;
      }
      this.messageBuffer = [];
      
      // 检查错误类型，只有真正的连接错误才重试
      const shouldRetry = this.shouldRetryConnection(error);
      
      if (shouldRetry && this.connectionRetries < this.maxRetries) {
        this.connectionRetries++;
        const delay = this.retryDelay * this.connectionRetries;
        
        this.showRetryMessage(`连接失败，${delay/1000}秒后自动重试 (${this.connectionRetries}/${this.maxRetries})...`);
        
        setTimeout(() => {
          this.retryLastMessage();
        }, delay);
      } else {
        // 超过最大重试次数或不应该重试
        this.isLoading = false;
        this.showErrorMessage(error);
      }
    },
    
    shouldRetryConnection(error) {
      // 判断是否应该重试连接
      // 只有在真正的连接问题时才重试
      
      // 如果错误是网络相关的，可以重试
      if (error && error.type) {
        switch (error.type) {
          case 'error':
            // 连接错误，可以重试
            return true;
          case 'timeout':
            // 超时错误，可以重试
            return true;
          case 'abort':
            // 用户主动取消，不重试
            return false;
          default:
            // 其他类型错误，谨慎处理，不重试
            return false;
        }
      }
      
      // 如果错误对象有特定的网络错误标识
      if (error && (error.code === 'NETWORK_ERROR' || error.message.includes('network'))) {
        return true;
      }
      
      // 默认情况下，如果没有收到任何数据且没有在传输过程中，可以重试
      return !this.hasReceivedData && !this.isReceivingData;
    },
    
    showRetryMessage(message) {
      // 显示重试消息
      if (this.currentResponse.includes('🤔 疾控监督专家正在思考中...')) {
        this.currentResponse = '';
      }
      this.appendToResponse(`🔄 ${message}\n\n`);
    },
    
    showErrorMessage(error) {
      // 显示用户友好的错误消息
      let errorMessage = '很抱歉，连接出现问题。';
      
      if (error.type === 'timeout') {
        errorMessage = '连接超时，请检查网络后重试。';
      } else if (error.type === 'error') {
        errorMessage = '服务暂时不可用，请稍后再试。';
      }
      
      // 如果没有收到任何有效响应，显示错误消息
      if (!this.currentResponse || this.currentResponse.includes('🤔 疾控监督专家正在思考中...')) {
        this.currentResponse = `❌ ${errorMessage}`;
        this.updateLastMessage();
      } else {
        this.appendToResponse(`\n\n❌ ${errorMessage}`);
      }
    },
    
    retryLastMessage() {
      // 找到最后一条用户消息并重新发送
      const chatMessages = this.chatStore.getInspectorChat(this.chatId);
      const lastUserMessage = [...chatMessages].reverse().find(msg => msg.role === 'user');
      
      if (lastUserMessage) {
        this.resendMessage(lastUserMessage.content);
      }
    },
    
    resendMessage(message) {
      // 重新发送消息
      this.resetMessageState();
      this.isLoading = true;
      this.currentResponse = '🔄 正在重新连接...';
      this.updateLastMessage();
      
      try {
        this.sseConnection = api.chatWithInspectorSSE(
          message,
          this.chatId,
          this.handleSSEMessage,
          this.handleSSEError,
          this.handleSSEComplete
        );
      } catch (error) {
        console.error('重发消息失败:', error);
        this.handleSSEError(error);
      }
    },
    
    handleSSEComplete() {
      // 清理定时器和缓冲区
      if (this.updateTimer) {
        clearTimeout(this.updateTimer);
        this.updateTimer = null;
      }
      this.messageBuffer = [];
      
      // 重置状态标记
      this.isReceivingData = false;
      this.connectionRetries = 0;
      
      // 计算响应时间
      if (this.lastMessageTime) {
        const responseTime = Date.now() - this.lastMessageTime;
        this.updateResponseTimeStats(responseTime);
      }
      
      // 标记完成
      this.isLoading = false;
      this.sseConnection = null;
      
      // 确保最终滚动
      this.smartScrollToBottom();
      
      // 开发环境日志
      if (process.env.NODE_ENV === 'development') {
        console.log('SSE连接正常完成，已收到数据:', this.hasReceivedData);
      }
    },
    
    updateResponseTimeStats(responseTime) {
      // 更新平均响应时间统计
      this.messageCount++;
      this.averageResponseTime = (this.averageResponseTime * (this.messageCount - 1) + responseTime) / this.messageCount;
      
      // 开发环境下记录性能指标
      if (process.env.NODE_ENV === 'development') {
        console.log(`响应时间: ${responseTime}ms, 平均响应时间: ${Math.round(this.averageResponseTime)}ms`);
      }
    },
    
    smartScrollToBottom() {
      // 智能滚动：检查用户是否接近底部
      const container = this.$refs.messagesContainer;
      if (!container) return;
      
      const isNearBottom = container.scrollTop + container.clientHeight >= container.scrollHeight - 100;
      
      if (isNearBottom || this.autoScroll) {
        this.scrollToBottom();
      }
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
    },

    async exportSessionToWord() {
      try {
        if (this.messages.length === 0) {
          alert('没有可导出的消息');
          return;
        }

        await exportToWord(this.messages, '疾控监督专家会话记录', this.chatId);
        console.log('Word导出成功');
      } catch (error) {
        console.error('导出Word失败:', error);
        alert('导出Word失败: ' + error.message);
      }
    },

    async exportSessionToPDF() {
      try {
        if (this.messages.length === 0) {
          alert('没有可导出的消息');
          return;
        }

        await exportToPDF(this.messages, '疾控监督专家会话记录', this.chatId);
        console.log('PDF导出成功');
      } catch (error) {
        console.error('导出PDF失败:', error);
        alert('导出PDF失败: ' + error.message);
      }
    },

    handleExportSuccess(format) {
      console.log(`${format} 导出成功`);
      // 可以添加成功提示，比如 toast 通知
    },

    handleExportError(errorMessage) {
      console.error('导出失败:', errorMessage);
      alert('导出失败: ' + errorMessage);
    },
    
    cleanupResources() {
      // 清理SSE连接
      if (this.sseConnection) {
        this.sseConnection.close();
        this.sseConnection = null;
      }
      
      // 清理定时器
      if (this.updateTimer) {
        clearTimeout(this.updateTimer);
        this.updateTimer = null;
      }
      
      // 清理缓冲区和状态
      this.messageBuffer = [];
      this.isReceivingData = false;
      this.hasReceivedData = false;
      this.connectionRetries = 0;
      
      // 重置加载状态
      this.isLoading = false;
    }
  },
  beforeUnmount() {
    // 组件销毁前清理资源
    this.cleanupResources();
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

.header-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.export-session-buttons {
  display: flex;
  gap: 8px;
}

.export-session-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 500;
}

.export-session-btn.export-word {
  background-color: #2b579a;
  color: white;
}

.export-session-btn.export-word:hover {
  background-color: #1e3f73;
  transform: translateY(-1px);
}

.export-session-btn.export-pdf {
  background-color: #dc3545;
  color: white;
}

.export-session-btn.export-pdf:hover {
  background-color: #c82333;
  transform: translateY(-1px);
}

.export-session-btn:active {
  transform: scale(0.95);
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

/* 功能亮点展示 */
.feature-highlights {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 15px;
  margin: 25px 0;
  padding: 20px;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 10px;
  border: 1px solid #dee2e6;
}

.feature-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 15px 10px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  cursor: default;
}

.feature-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(66, 133, 244, 0.1);
}

.feature-icon {
  font-size: 1.4rem;
  margin-bottom: 4px;
}

.feature-text {
  font-size: 0.85rem;
  font-weight: 500;
  color: #2c3e50;
  text-align: center;
  line-height: 1.3;
}

/* 快速提示 */
.quick-tips {
  margin-top: 25px;
  padding: 20px;
  background: rgba(66, 133, 244, 0.05);
  border-radius: 10px;
  border: 1px solid rgba(66, 133, 244, 0.1);
  text-align: left;
}

.tips-title {
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 12px;
  font-size: 0.95rem;
}

.tips-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.tips-list li {
  padding: 6px 0;
  color: #4a5568;
  font-size: 0.9rem;
  position: relative;
  padding-left: 16px;
}

.tips-list li::before {
  content: "•";
  color: var(--primary-color);
  font-weight: bold;
  position: absolute;
  left: 0;
  top: 6px;
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
  
  /* 响应式：功能亮点 */
  .feature-highlights {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    margin: 20px 0;
    padding: 15px;
  }
  
  .feature-item {
    padding: 12px 8px;
  }
  
  .feature-icon {
    font-size: 1.2rem;
  }
  
  .feature-text {
    font-size: 0.8rem;
  }
  
  /* 响应式：快速提示 */
  .quick-tips {
    margin-top: 20px;
    padding: 15px;
  }
  
  .tips-title {
    font-size: 0.9rem;
  }
  
  .tips-list li {
    font-size: 0.85rem;
    padding: 4px 0;
  }
  
  .welcome-message {
    padding: 20px;
    max-width: 100%;
  }
}
</style> 