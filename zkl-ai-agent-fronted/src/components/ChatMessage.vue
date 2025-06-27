<template>
  <div class="message" :class="role === 'user' ? 'message-user' : 'message-ai'">
    <div class="message-avatar">
      <img :src="avatarSrc" :alt="role === 'user' ? '用户头像' : 'AI头像'" />
    </div>
    <div class="message-content">
      <div class="message-header">
        <span class="message-name">{{ role === 'user' ? '您' : aiName }}</span>
        <span class="message-time">{{ formattedTime }}</span>
      </div>
      <div class="message-text" v-html="formattedContent"></div>
    </div>
  </div>
</template>

<script>
import { formatDateTime } from '../utils/helpers';
import { marked } from 'marked';
import hljs from 'highlight.js';
import 'highlight.js/styles/github.css';
import userAvatar from '../assets/user-avatar.svg';

// 配置marked使用highlight.js
marked.setOptions({
  highlight: function(code, lang) {
    const language = hljs.getLanguage(lang) ? lang : 'plaintext';
    return hljs.highlight(code, { language }).value;
  },
  langPrefix: 'hljs language-', // highlight.js css expects a language-* class
  breaks: true, // 允许回车换行
  gfm: true, // 允许GitHub风格的markdown
  headerIds: false, // 不自动生成header IDs
  mangle: false, // 不转义html标记
  sanitize: false, // 不过滤html标签
});

export default {
  name: 'ChatMessage',
  props: {
    role: {
      type: String,
      required: true,
      validator: (value) => ['user', 'assistant'].includes(value)
    },
    content: {
      type: String,
      required: true
    },
    timestamp: {
      type: String,
      required: true
    },
    aiName: {
      type: String,
      default: 'AI助手'
    },
    aiAvatar: {
      type: String,
      default: ''
    }
  },
  computed: {
    avatarSrc() {
      return this.role === 'user' 
        ? userAvatar
        : this.aiAvatar;
    },
    formattedTime() {
      return formatDateTime(this.timestamp);
    },
    formattedContent() {
      try {
        // 使用marked库解析Markdown
        return marked(this.content);
      } catch (error) {
        console.error('Failed to parse markdown:', error);
        return this.content;
      }
    }
  }
}
</script>

<style scoped>
.message {
  display: flex;
  margin-bottom: 24px;
  max-width: 85%;
}

.message-user {
  margin-left: auto;
  flex-direction: row-reverse;
}

.message-ai {
  margin-right: auto;
}

.message-avatar {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  margin: 0 12px;
}

.message-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.message-content {
  background-color: var(--white);
  border-radius: 12px;
  padding: 12px 16px;
  box-shadow: var(--shadow);
  min-width: 200px;
}

.message-user .message-content {
  background-color: var(--primary-color);
  color: white;
}

.message-ai .message-content {
  background-color: var(--white);
  text-align: left;
}

.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
}

.message-name {
  font-weight: bold;
}

.message-time {
  color: rgba(0, 0, 0, 0.5);
  font-size: 12px;
}

.message-user .message-time {
  color: rgba(255, 255, 255, 0.7);
}

.message-text {
  word-break: break-word;
  line-height: 1.5;
}

/* 修改深层样式，解决scoped样式无法直接应用到v-html内容的问题 */
.message-text :deep(pre) {
  background-color: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
  overflow-x: auto;
  margin: 10px 0;
}

.message-text :deep(code) {
  font-family: Consolas, Monaco, 'Andale Mono', monospace;
}

.message-text :deep(p) {
  margin-bottom: 8px;
}

.message-text :deep(h1),
.message-text :deep(h2),
.message-text :deep(h3),
.message-text :deep(h4),
.message-text :deep(h5),
.message-text :deep(h6) {
  margin-top: 16px;
  margin-bottom: 8px;
  font-weight: 600;
}

.message-text :deep(ul),
.message-text :deep(ol) {
  padding-left: 20px;
  margin-bottom: 8px;
}

.message-text :deep(li) {
  margin-bottom: 4px;
}

.message-text :deep(table) {
  border-collapse: collapse;
  margin: 12px 0;
  width: 100%;
}

.message-text :deep(th),
.message-text :deep(td) {
  border: 1px solid #ddd;
  padding: 8px;
  text-align: left;
}

.message-text :deep(th) {
  background-color: #f2f2f2;
  font-weight: bold;
}

.message-text :deep(blockquote) {
  border-left: 4px solid var(--light-gray);
  margin: 8px 0;
  padding: 4px 12px;
  color: #666;
}

.message-user .message-text :deep(code) {
  background-color: rgba(255, 255, 255, 0.2);
}

@media (max-width: 768px) {
  .message {
    max-width: 95%;
  }
  
  .message-avatar {
    width: 32px;
    height: 32px;
  }
}
</style> 