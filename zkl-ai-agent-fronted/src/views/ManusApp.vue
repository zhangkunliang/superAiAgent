<template>
  <div class="chat-container">
    <header class="chat-header">
      <div class="container">
        <div class="header-content">
          <router-link to="/" class="back-button">
            <span class="back-icon">←</span> 返回
          </router-link>
          <h1 class="chat-title">超级智能体</h1>
          <div class="header-right">
            <div class="export-session-buttons" v-if="messages.length > 0">
              <button
                @click="exportSessionToWord"
                class="export-session-btn export-word"
                title="导出整个会话为Word文档"
              >
                📄 Word
              </button>
              <button
                @click="exportSessionToPDF"
                class="export-session-btn export-pdf"
                title="导出整个会话为PDF文档"
              >
                📋 PDF
              </button>
              <button
                @click="exportSessionToHTML"
                class="export-session-btn export-html"
                title="导出整个会话为HTML文档"
              >
                🌐 HTML
              </button>
              <button
                @click="exportSessionToText"
                class="export-session-btn export-text"
                title="导出整个会话为文本文档"
              >
                📝 文本
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
                <img :src="aiAvatar" alt="超级智能体" class="welcome-avatar" />
                <h2>欢迎使用超级智能体</h2>
                <p>我是您的多功能AI助手，可以执行复杂任务，提供全面的问题解决方案。请输入您的问题或指令。</p>
                <div class="feature-highlights">
                  <div class="feature-item">
                    <span class="feature-icon">🔧</span>
                    <span>工具调用</span>
                  </div>
                  <div class="feature-item">
                    <span class="feature-icon">🧠</span>
                    <span>智能推理</span>
                  </div>
                  <div class="feature-item">
                    <span class="feature-icon">📊</span>
                    <span>数据分析</span>
                  </div>
                  <div class="feature-item">
                    <span class="feature-icon">🖼️</span>
                    <span>图像理解</span>
                  </div>
                </div>
              </div>
            </div>
            
            <!-- 简化的加载状态显示 -->
            <div v-if="isLoading && currentExecutionStep > 0" class="execution-status">
              <div class="status-indicator">
                <div class="loading-spinner"></div>
                <span class="status-text">正在执行第 {{ currentExecutionStep }} / {{ maxSteps }} 步...</span>
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
              @export-success="handleExportSuccess"
              @export-error="handleExportError"
            />
          </div>
          
          <div class="chat-input-area">
            <!-- 图片预览区域 -->
            <div v-if="selectedImage" class="image-preview-container">
              <div class="image-preview">
                <img :src="selectedImage.preview" alt="预览图片" class="preview-image" />
                <div class="image-info">
                  <span class="image-name">{{ selectedImage.name }}</span>
                  <span class="image-size">{{ formatFileSize(selectedImage.size) }}</span>
                </div>
                <button @click="removeImage" class="remove-image-btn" title="移除图片">
                  ✕
                </button>
              </div>
            </div>

            <div class="input-container">
              <div class="input-wrapper">
                <textarea
                  ref="messageInput"
                  v-model="userInput"
                  class="message-input"
                  placeholder="请输入您的问题或指令，支持粘贴图片..."
                  @keydown.enter.prevent="sendMessage"
                  @paste="handlePaste"
                  @drop="handleDrop"
                  @dragover.prevent
                  @dragenter.prevent
                  :disabled="isLoading"
                ></textarea>

                <div class="input-actions">
                  <input
                    ref="fileInput"
                    type="file"
                    accept="image/*"
                    @change="handleFileSelect"
                    style="display: none"
                  />
                  <button
                    @click="$refs.fileInput.click()"
                    class="attach-button"
                    title="上传图片"
                    :disabled="isLoading"
                  >
                    📎
                  </button>
                  <button
                    @click="sendMessage"
                    class="send-button"
                    :disabled="isLoading || (!userInput.trim() && !selectedImage)"
                  >
                    {{ isLoading ? '发送中...' : '发送' }}
                  </button>
                </div>
              </div>
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
import { exportToWord, exportToPDF } from '../utils/exportUtils';
import { exportToHTML, exportToText, exportToMarkdown } from '../utils/simpleExport';

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
      sessionToDelete: null,
      isSidebarCollapsed: false,
      selectedImage: null, // 选中的图片信息
      maxImageSize: 10 * 1024 * 1024, // 10MB 最大图片大小
      supportedImageTypes: ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'],
      
      // 新增：消息处理优化相关
      messageBuffer: [], // 消息缓冲区
      updateTimer: null, // 更新定时器
      executionSteps: [], // 执行步骤列表
      currentExecutionStep: 0, // 当前执行步骤
      maxSteps: 20, // 最大步骤数
      autoScroll: true, // 自动滚动开关
      currentToolInfo: null, // 当前工具信息
      
      // 新增：错误处理相关
      connectionRetries: 0,
      maxRetries: 3,
      retryDelay: 1000,
      isReceivingData: false, // 标记是否正在接收数据
      hasReceivedData: false, // 标记是否已收到数据
      
      // 新增：性能监控
      lastMessageTime: null,
      messageCount: 0,
      averageResponseTime: 0
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
    
    // 从本地存储加载侧边栏状态
    const savedState = localStorage.getItem('manusSidebarCollapsed');
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
      // 检查是否有内容可发送
      if ((!this.userInput.trim() && !this.selectedImage) || this.isLoading) return;

      const message = this.userInput.trim() || '请分析这张图片';
      const imageData = this.selectedImage;

      // 重置状态
      this.resetMessageState();
      
      // 记录发送时间用于性能监控
      this.lastMessageTime = Date.now();
      this.messageCount++;

      // 清空输入
      this.userInput = '';
      this.selectedImage = null;

      // 构建用户消息内容（包含图片信息）
      let userMessageContent = message;
      if (imageData) {
        userMessageContent += `\n[图片: ${imageData.name}]`;
      }

      // 添加用户消息到聊天记录
      this.chatStore.addManusUserMessage(this.chatId, userMessageContent);

      // 开始加载状态和初始化
      this.isLoading = true;
      this.currentResponse = '';

      // 创建空的占位回复，等待后台消息填充
      this.chatStore.addManusAiMessage(this.chatId, this.currentResponse);

      // 发送请求（根据是否有图片选择不同的API）
      try {
        if (imageData) {
          this.sseConnection = api.chatWithManusMultiModalSSE(
            message,
            imageData.file,
            this.handleSSEMessage,
            this.handleSSEError,
            this.handleSSEComplete
          );
        } else {
          this.sseConnection = api.chatWithManusSSE(
            message,
            this.handleSSEMessage,
            this.handleSSEError,
            this.handleSSEComplete
          );
        }
      } catch (error) {
        console.error('发送消息失败:', error);
        this.handleSSEError(error);
      }
    },
    
    resetMessageState() {
      // 重置消息相关状态
      this.currentResponse = '';
      this.executionSteps = [];
      this.currentExecutionStep = 0;
      this.messageBuffer = [];
      this.connectionRetries = 0;
      this.currentToolInfo = null;
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
        
        // 调试：记录接收数据（生产环境可删除）
        if (process.env.NODE_ENV === 'development') {
          console.log('SSE received:', data.substring(0, 100) + '...');
        }

        // 尝试解析JSON格式的消息
        let messageData;
        try {
          messageData = JSON.parse(data);
        } catch (e) {
          // 如果不是JSON格式，按原文本处理
          console.log('Non-JSON message, treating as plain text:', data);
          this.handlePlainTextMessage(data);
          return;
        }

        // 处理结构化消息
        this.handleStructuredMessage(messageData);

      } catch (error) {
        console.error('Error handling SSE message:', error);
        // 降级处理：按纯文本处理，但不触发重连
        this.handlePlainTextMessage(data);
      }
    },

    handleStructuredMessage(messageData) {
      const { type, content, currentStep, totalSteps, toolInfo, finished } = messageData;
      
      // 根据消息类型处理
      switch (type) {
        case 'THINKING':
          this.appendToResponse(`💭 **思考中**\n${content}\n\n`);
          break;
          
        case 'TOOL_CALLING':
          this.appendToResponse(`🛠️ **工具调用**\n${content}\n\n`);
          if (toolInfo) {
            this.currentToolInfo = toolInfo;
          }
          break;
          
        case 'CONTENT':
          this.appendToResponse(content + '\n\n');
          break;
          
        case 'STEP_PROGRESS':
          if (currentStep && totalSteps) {
            this.updateStepProgress(currentStep, totalSteps, content);
          }
          this.appendToResponse(content + '\n\n');
          break;
          
        case 'COMPLETE':
          this.appendToResponse(`${content}\n\n`);
          this.markAsComplete();
          break;
          
        case 'ERROR':
          this.appendToResponse(`❌ **错误**\n${content}\n\n`);
          this.markAsError();
          break;
          
        default:
          // 未知类型，按内容处理
          this.appendToResponse(content + '\n\n');
      }

      // 智能滚动
      this.smartScrollToBottom();
    },

    handlePlainTextMessage(data) {
      // 兼容旧格式的纯文本消息
      this.appendToResponse(data);
      this.smartScrollToBottom();
    },

    appendToResponse(content) {
      if (content && content.trim()) {
        this.currentResponse += content;
        this.updateLastMessage();
      }
    },
    
    updateStepProgress(stepNumber, totalSteps, content) {
      // 简化的步骤进度更新
      this.currentExecutionStep = stepNumber;
      this.maxSteps = totalSteps;
      
      // 可以在这里添加步骤进度的可视化更新逻辑
      // 但现在我们主要依靠统一的消息流
    },
    
    markAsComplete() {
      // 标记任务完成
      this.isLoading = false;
      this.currentExecutionStep = 0;
      this.executionSteps = [];
    },
    
    markAsError() {
      // 标记任务出错
      this.isLoading = false;
      this.currentExecutionStep = 0;
      this.executionSteps = [];
    },
    
    updateLastMessage() {
      const chatMessages = this.chatStore.getManusChat(this.chatId);
      const lastMessage = chatMessages[chatMessages.length - 1];

      if (lastMessage && lastMessage.role === 'assistant') {
        lastMessage.content = this.currentResponse;
      }
    },
    
    smartScrollToBottom() {
      if (!this.$refs.messagesContainer) return;
      
      const container = this.$refs.messagesContainer;
      const isNearBottom = container.scrollTop + container.clientHeight >= container.scrollHeight - 100;
      
      // 只有当用户在底部附近时才自动滚动
      if (isNearBottom || this.autoScroll) {
        this.$nextTick(() => {
          container.scrollTop = container.scrollHeight;
        });
      }
    },
    
    handleSSEError(error) {
      console.error('SSE连接错误:', error);
      
      // 重要：如果已经在接收数据过程中，或者已经收到了部分数据，不应该重试
      // 这通常表示连接正常，只是数据传输过程中的临时问题
      if (this.isReceivingData || this.hasReceivedData) {
        console.log('正在传输数据过程中出现错误，不触发重连机制');
        this.isLoading = false;
        
        // 如果有部分响应内容，保留它们，只添加错误提示
        if (this.currentResponse && this.currentResponse.trim() !== '') {
          this.currentResponse += '\n\n⚠️ 传输完成，如有遗漏请重新提问。';
          this.updateLastMessage();
        } else {
          this.showErrorMessage(error);
        }
        return;
      }
      
      this.isLoading = false;
      
      // 清理定时器和缓冲区
      if (this.updateTimer) {
        clearTimeout(this.updateTimer);
        this.updateTimer = null;
      }
      this.flushMessageBuffer();
      
      // 检查错误类型，只有真正的连接错误才重试
      const shouldRetry = this.shouldRetryConnection(error);
      
      if (shouldRetry && this.connectionRetries < this.maxRetries) {
        this.connectionRetries++;
        const retryDelay = this.retryDelay * this.connectionRetries;
        
        // 显示重试提示
        const retryMessage = `连接中断，${retryDelay/1000}秒后自动重试 (${this.connectionRetries}/${this.maxRetries})...`;
        this.showRetryMessage(retryMessage);
        
        setTimeout(() => {
          this.retryLastMessage();
        }, retryDelay);
      } else {
        // 达到最大重试次数或不应该重试，显示错误信息
        this.showErrorMessage(error);
        this.connectionRetries = 0; // 重置重试计数
      }
    },
    
    shouldRetryConnection(error) {
      // 判断是否应该重试连接（与InspectorApp相同的逻辑）
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
      // 如果没有收到任何响应，添加重试消息
      if (!this.currentResponse || this.currentResponse.trim() === '') {
        this.chatStore.addManusAiMessage(this.chatId, `🔄 ${message}`);
      } else {
        // 更新最后的消息内容
        this.currentResponse += `\n\n🔄 ${message}`;
        this.updateLastMessage();
      }
    },
    
    showErrorMessage(error) {
      const errorTypes = {
        'TypeError': '网络连接错误，请检查网络设置',
        'AbortError': '请求被中断，请重新发送消息',
        'TimeoutError': '请求超时，请稍后再试'
      };
      
      const friendlyMessage = errorTypes[error.name] || '连接出现问题，请稍后再试';
      const fullErrorMessage = `❌ **连接失败**\n\n${friendlyMessage}\n\n*如果问题持续，请刷新页面或联系管理员*`;
      
      if (!this.currentResponse || this.currentResponse.trim() === '') {
        this.chatStore.addManusAiMessage(this.chatId, fullErrorMessage);
      } else {
        this.currentResponse += `\n\n${fullErrorMessage}`;
        this.updateLastMessage();
      }
    },
    
    retryLastMessage() {
      // 获取最后一条用户消息
      const messages = this.chatStore.getManusChat(this.chatId);
      const lastUserMessage = [...messages].reverse().find(msg => msg.role === 'user');
      
      if (lastUserMessage) {
        // 重新发送最后一条消息
        const message = lastUserMessage.content;
        this.resendMessage(message);
      }
    },
    
    resendMessage(message) {
      this.isLoading = true;
      this.currentResponse = '';
      this.executionSteps = [];
      this.currentExecutionStep = 0;
      this.messageBuffer = [];
      
      // 创建新的占位回复
      this.chatStore.addManusAiMessage(this.chatId, this.currentResponse);
      
      // 重新发送请求
      this.sseConnection = api.chatWithManusSSE(
        message,
        this.handleSSEMessage,
        this.handleSSEError,
        this.handleSSEComplete
      );
    },
    
    handleSSEComplete() {
      this.isLoading = false;
      this.sseConnection = null;
      
      // 清理定时器和缓冲区
      if (this.updateTimer) {
        clearTimeout(this.updateTimer);
        this.updateTimer = null;
      }
      this.flushMessageBuffer();
      
      // 重置状态标记
      this.isReceivingData = false;
      this.connectionRetries = 0;
      
      // 计算响应时间
      if (this.lastMessageTime) {
        const responseTime = Date.now() - this.lastMessageTime;
        this.updateResponseTimeStats(responseTime);
      }
      
      // 标记步骤完成
      if (this.executionSteps.length > 0) {
        this.currentResponse += '\n\n✅ **任务执行完成**';
        this.updateLastMessage();
      }
      
      // 开发环境日志
      if (process.env.NODE_ENV === 'development') {
        console.log('SSE连接正常完成，已收到数据:', this.hasReceivedData);
      }
      
      // 自动滚动到底部
      this.$nextTick(() => {
        this.scrollToBottom();
      });
    },
    
    updateResponseTimeStats(responseTime) {
      // 更新平均响应时间统计
      this.averageResponseTime = ((this.averageResponseTime * (this.messageCount - 1)) + responseTime) / this.messageCount;
      
      // 开发环境下显示性能信息
      if (process.env.NODE_ENV === 'development') {
        console.log(`响应时间: ${responseTime}ms, 平均: ${Math.round(this.averageResponseTime)}ms`);
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
    },
    
    toggleSidebar() {
      this.isSidebarCollapsed = !this.isSidebarCollapsed;
      // 保存用户偏好到本地存储
      localStorage.setItem('manusSidebarCollapsed', this.isSidebarCollapsed);
    },

    async exportSessionToWord() {
      try {
        if (this.messages.length === 0) {
          alert('没有可导出的消息');
          return;
        }

        try {
          await exportToWord(this.messages, '超级智能体会话记录', this.chatId);
          console.log('Word导出成功');
        } catch (wordError) {
          console.warn('Word导出失败，使用HTML格式:', wordError);
          await exportToHTML(this.messages, '超级智能体会话记录', this.chatId);
          alert('Word导出失败，已自动使用HTML格式导出');
        }
      } catch (error) {
        console.error('导出失败:', error);
        alert('导出失败: ' + error.message);
      }
    },

    async exportSessionToPDF() {
      try {
        if (this.messages.length === 0) {
          alert('没有可导出的消息');
          return;
        }

        try {
          await exportToPDF(this.messages, '超级智能体会话记录', this.chatId);
          console.log('PDF导出成功');
        } catch (pdfError) {
          console.warn('PDF导出失败，使用HTML格式:', pdfError);
          await exportToHTML(this.messages, '超级智能体会话记录', this.chatId);
          alert('PDF导出失败，已自动使用HTML格式导出');
        }
      } catch (error) {
        console.error('导出失败:', error);
        alert('导出失败: ' + error.message);
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

    // 图片处理相关方法
    handleFileSelect(event) {
      const file = event.target.files[0];
      if (file) {
        this.processImageFile(file);
      }
      // 清空input，允许重复选择同一文件
      event.target.value = '';
    },

    handlePaste(event) {
      const items = event.clipboardData?.items;
      if (!items) return;

      for (let i = 0; i < items.length; i++) {
        const item = items[i];
        if (item.type.startsWith('image/')) {
          event.preventDefault();
          const file = item.getAsFile();
          if (file) {
            this.processImageFile(file);
          }
          break;
        }
      }
    },

    handleDrop(event) {
      event.preventDefault();
      const files = event.dataTransfer?.files;
      if (!files || files.length === 0) return;

      const file = files[0];
      if (file.type.startsWith('image/')) {
        this.processImageFile(file);
      } else {
        alert('请拖拽图片文件');
      }
    },

    processImageFile(file) {
      // 验证文件类型
      if (!this.supportedImageTypes.includes(file.type)) {
        alert('不支持的图片格式。支持的格式：JPEG, PNG, GIF, WebP');
        return;
      }

      // 验证文件大小
      if (file.size > this.maxImageSize) {
        alert(`图片文件过大。最大支持 ${this.formatFileSize(this.maxImageSize)}`);
        return;
      }

      // 创建预览
      const reader = new FileReader();
      reader.onload = (e) => {
        this.selectedImage = {
          file: file,
          name: file.name,
          size: file.size,
          type: file.type,
          preview: e.target.result
        };
      };
      reader.readAsDataURL(file);
    },

    removeImage() {
      this.selectedImage = null;
    },

    formatFileSize(bytes) {
      if (bytes === 0) return '0 Bytes';
      const k = 1024;
      const sizes = ['Bytes', 'KB', 'MB', 'GB'];
      const i = Math.floor(Math.log(bytes) / Math.log(k));
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    },

    async exportSessionToHTML() {
      try {
        if (this.messages.length === 0) {
          alert('没有可导出的消息');
          return;
        }

        await exportToHTML(this.messages, '超级智能体会话记录', this.chatId);
        console.log('HTML导出成功');
      } catch (error) {
        console.error('导出HTML失败:', error);
        alert('导出HTML失败: ' + error.message);
      }
    },

    async exportSessionToText() {
      try {
        if (this.messages.length === 0) {
          alert('没有可导出的消息');
          return;
        }

        await exportToText(this.messages, '超级智能体会话记录', this.chatId);
        console.log('文本导出成功');
      } catch (error) {
        console.error('导出文本失败:', error);
        alert('导出文本失败: ' + error.message);
      }
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

.export-session-btn.export-html {
  background-color: #17a2b8;
  color: white;
}

.export-session-btn.export-html:hover {
  background-color: #138496;
  transform: translateY(-1px);
}

.export-session-btn.export-text {
  background-color: #6c757d;
  color: white;
}

.export-session-btn.export-text:hover {
  background-color: #5a6268;
  transform: translateY(-1px);
}

.export-session-btn:active {
  transform: scale(0.95);
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
  color: var(--secondary-color);
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

.arrow-icon {
  font-size: 14px;
  line-height: 1;
  font-weight: bold;
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

/* 图片预览区域 */
.image-preview-container {
  margin-bottom: 10px;
}

.image-preview {
  display: flex;
  align-items: center;
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 10px;
  position: relative;
}

.preview-image {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
  margin-right: 12px;
}

.image-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.image-name {
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
  word-break: break-all;
}

.image-size {
  font-size: 0.85rem;
  color: #666;
}

.remove-image-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  background: #dc3545;
  color: white;
  border: none;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  cursor: pointer;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s;
}

.remove-image-btn:hover {
  background: #c82333;
}

/* 输入容器 */
.input-container {
  display: flex;
  gap: 10px;
}

.input-wrapper {
  flex: 1;
  display: flex;
  border: 1px solid var(--light-gray);
  border-radius: 8px;
  overflow: hidden;
  transition: border-color 0.2s;
}

.input-wrapper:focus-within {
  border-color: var(--secondary-color);
}

.message-input {
  flex: 1;
  padding: 12px 15px;
  border: none;
  resize: none;
  height: 60px;
  font-family: "Microsoft YaHei", sans-serif;
  font-size: 1rem;
  background: transparent;
}

.message-input:focus {
  outline: none;
}

.message-input::placeholder {
  color: #999;
}

/* 拖拽状态 */
.message-input:dragover {
  background-color: #f0f8ff;
}

.input-actions {
  display: flex;
  align-items: stretch;
  border-left: 1px solid var(--light-gray);
}

.attach-button {
  background: transparent;
  border: none;
  padding: 0 12px;
  cursor: pointer;
  font-size: 16px;
  color: #666;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.attach-button:hover:not(:disabled) {
  background-color: #f8f9fa;
  color: var(--secondary-color);
}

.attach-button:disabled {
  color: #ccc;
  cursor: not-allowed;
}

.send-button {
  background-color: var(--secondary-color);
  color: white;
  border: none;
  padding: 0 20px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
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
  color: var(--secondary-color);
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
  stroke: var(--secondary-color);
}

/* 功能特色展示 */
.feature-highlights {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
  margin-top: 20px;
  padding: 20px;
  background: rgba(66, 133, 244, 0.05);
  border-radius: 12px;
  border: 1px solid rgba(66, 133, 244, 0.1);
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px;
  background: white;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 500;
  color: #333;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.feature-icon {
  font-size: 1.2rem;
}

/* 简化的执行状态显示 */
.execution-status {
  display: flex;
  justify-content: center;
  padding: 15px;
  margin: 10px 0;
  background: rgba(66, 133, 244, 0.05);
  border-radius: 8px;
  border: 1px solid rgba(66, 133, 244, 0.1);
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 12px;
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #e3e3e3;
  border-top: 2px solid var(--primary-color);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.status-text {
  color: #2c3e50;
  font-size: 0.9rem;
  font-weight: 500;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .feature-highlights {
    grid-template-columns: 1fr;
    gap: 10px;
    padding: 15px;
  }
  
  .execution-status {
    margin: 10px 0;
    padding: 12px;
  }
  
  .status-text {
    font-size: 0.85rem;
  }
}
</style>