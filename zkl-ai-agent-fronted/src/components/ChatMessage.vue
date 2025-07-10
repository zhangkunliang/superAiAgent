<template>
  <div class="message" :class="role === 'user' ? 'message-user' : 'message-ai'">
    <div class="message-avatar">
      <img :src="avatarSrc" :alt="role === 'user' ? '用户头像' : 'AI头像'" />
    </div>
    <div class="message-content">
      <div class="message-header">
        <span class="message-name">{{ role === 'user' ? '您' : aiName }}</span>
        <div class="message-actions">
          <span class="message-time">{{ formattedTime }}</span>
          <div v-if="role === 'assistant'" class="export-buttons">
            <button
              @click="exportToWord"
              class="export-btn export-word"
              title="导出为Word文档"
            >
              📄 Word
            </button>
            <button
              @click="exportToPDF"
              class="export-btn export-pdf"
              title="导出为PDF文档"
            >
              📋 PDF
            </button>
          </div>
        </div>
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
import { exportToWord, exportToPDF } from '../utils/exportUtils';
import { exportToHTML, exportToText, exportToMarkdown } from '../utils/simpleExport';

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
  pedantic: false, // 不使用原始markdown.pl的怪异行为
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
        if (!this.content) return '';

        console.log('Processing content:', this.content.substring(0, 100) + '...');
        console.log('Content includes \\n:', this.content.includes('\n'));
        console.log('Content includes \\\\n:', this.content.includes('\\n'));

        // 创建一个更强健的换行符处理函数
        let processedContent = this.content;

        // 首先尝试解码可能的 JSON 转义字符
        try {
          // 如果内容看起来像是 JSON 转义的，尝试解码
          if (processedContent.includes('\\n') || processedContent.includes('\\r') || processedContent.includes('\\t')) {
            // 创建一个临时的 JSON 字符串来解码转义字符
            const tempJson = '"' + processedContent.replace(/"/g, '\\"') + '"';
            const decoded = JSON.parse(tempJson);
            console.log('Successfully decoded JSON escapes');
            processedContent = decoded;
          }
        } catch (e) {
          console.log('JSON decode failed, continuing with manual replacement');
        }

        // 处理各种换行符格式（按优先级顺序）
        const newlineReplacements = [
          [/\\r\\n/g, '<br>'],  // Windows 转义换行符
          [/\\n/g, '<br>'],     // 转义的换行符
          [/\\r/g, '<br>'],     // 转义的回车符
          [/\r\n/g, '<br>'],    // Windows 换行符
          [/\n/g, '<br>'],      // Unix 换行符
          [/\r/g, '<br>']       // Mac 换行符
        ];

        for (const [pattern, replacement] of newlineReplacements) {
          if (pattern.test(processedContent)) {
            console.log('Replacing pattern:', pattern);
            processedContent = processedContent.replace(pattern, replacement);
          }
        }

        // 处理基本的 markdown 语法
        const markdownReplacements = [
          [/\*\*(.*?)\*\*/g, '<strong>$1</strong>'],  // 粗体
          [/(?<!\*)\*([^*]+)\*(?!\*)/g, '<em>$1</em>'], // 斜体
          [/`([^`]+)`/g, '<code>$1</code>']  // 行内代码
        ];

        for (const [pattern, replacement] of markdownReplacements) {
          if (pattern.test(processedContent)) {
            processedContent = processedContent.replace(pattern, replacement);
          }
        }

        // 清理多余的连续 <br> 标签
        processedContent = processedContent.replace(/(<br>\s*){3,}/g, '<br><br>');

        console.log('Final processed content:', processedContent.substring(0, 100) + '...');
        return processedContent;

      } catch (error) {
        console.error('Failed to parse content:', error);
        // 最后的备用方案：强制替换所有可能的换行符
        return this.content
          .replace(/\\r\\n/g, '<br>')
          .replace(/\\n/g, '<br>')
          .replace(/\\r/g, '<br>')
          .replace(/\r\n/g, '<br>')
          .replace(/\n/g, '<br>')
          .replace(/\r/g, '<br>');
      }
    }
  },
  methods: {
    async exportToWord() {
      try {
        const message = {
          role: this.role,
          content: this.content,
          timestamp: this.timestamp
        };

        // 尝试 Word 导出，失败则降级为 HTML
        try {
          await exportToWord([message], `${this.aiName}回复`, '');
          this.$emit('export-success', 'Word');
        } catch (wordError) {
          console.warn('Word export failed, using HTML export:', wordError);
          await exportToHTML([message], `${this.aiName}回复`, '');
          this.$emit('export-success', 'HTML (Word替代)');
        }
      } catch (error) {
        console.error('导出失败:', error);
        this.$emit('export-error', error.message);
      }
    },

    async exportToPDF() {
      try {
        const message = {
          role: this.role,
          content: this.content,
          timestamp: this.timestamp
        };

        // 尝试 PDF 导出，失败则降级为 HTML
        try {
          await exportToPDF([message], `${this.aiName}回复`, '');
          this.$emit('export-success', 'PDF');
        } catch (pdfError) {
          console.warn('PDF export failed, using HTML export:', pdfError);
          await exportToHTML([message], `${this.aiName}回复`, '');
          this.$emit('export-success', 'HTML (PDF替代)');
        }
      } catch (error) {
        console.error('导出失败:', error);
        this.$emit('export-error', error.message);
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
  align-items: flex-start;
  margin-bottom: 8px;
  font-size: 14px;
}

.message-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 5px;
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

.export-buttons {
  display: flex;
  gap: 5px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.message:hover .export-buttons {
  opacity: 1;
}

.export-btn {
  padding: 4px 8px;
  border: none;
  border-radius: 4px;
  font-size: 11px;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 2px;
}

.export-word {
  background-color: #2b579a;
  color: white;
}

.export-word:hover {
  background-color: #1e3f73;
}

.export-pdf {
  background-color: #dc3545;
  color: white;
}

.export-pdf:hover {
  background-color: #c82333;
}

.export-btn:active {
  transform: scale(0.95);
}

.message-text {
  word-break: break-word;
  line-height: 1.5;
  overflow-wrap: break-word; /* 确保长单词能够换行 */
}

/* 修改深层样式，解决scoped样式无法直接应用到v-html内容的问题 */
.message-text :deep(pre) {
  background-color: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
  overflow-x: auto;
  margin: 10px 0;
  white-space: pre-wrap; /* 保留换行符和空格 */
}

.message-text :deep(code) {
  font-family: Consolas, Monaco, 'Andale Mono', monospace;
}

.message-text :deep(p) {
  margin-bottom: 8px;
}

.message-text :deep(p:last-child) {
  margin-bottom: 0;
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