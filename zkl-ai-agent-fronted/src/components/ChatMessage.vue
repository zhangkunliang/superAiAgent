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
  data() {
    return {
      isStreamComplete: false,
      lastProcessedLength: 0,
      parseCache: {
        hasCompleteJSON: false,
        pendingContent: '',
        lastParseTime: 0
      }
    };
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

        // 检查是否是流式输出的新内容
        const isNewContent = this.content.length > this.lastProcessedLength;
        const currentTime = Date.now();

        // 流式输出优化：避免频繁解析
        if (isNewContent && currentTime - this.parseCache.lastParseTime < 100) {
          // 如果距离上次解析时间太短，延迟解析
          this.scheduleDelayedParse();
          return this.parseCache.pendingContent || this.content;
        }

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

        // 特殊处理：步骤式输出格式优化
        processedContent = this.formatStepOutput(processedContent);

        // 流式输出的智能解析
        processedContent = this.parseStreamContent(processedContent);

        // 更新解析缓存
        this.lastProcessedLength = this.content.length;
        this.parseCache.lastParseTime = currentTime;
        this.parseCache.pendingContent = processedContent;

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
    },

    // 延迟解析调度
    scheduleDelayedParse() {
      if (this.parseTimeout) {
        clearTimeout(this.parseTimeout);
      }
      this.parseTimeout = setTimeout(() => {
        this.$forceUpdate(); // 强制重新计算
      }, 150);
    },

    // 流式内容智能解析
    parseStreamContent(content) {
      // 检查内容是否包含完整的JSON代码块
      const hasCompleteJSONBlock = this.hasCompleteJSONBlocks(content);

      // 如果包含完整的JSON块或者内容看起来已经完成，进行完整解析
      if (hasCompleteJSONBlock || this.looksLikeCompleteContent(content)) {
        return this.parseCompleteContent(content);
      } else {
        // 否则进行安全的部分解析
        return this.parsePartialContent(content);
      }
    },

    // 检查是否包含完整的JSON代码块
    hasCompleteJSONBlocks(content) {
      const jsonBlockRegex = /```json\s*([\s\S]*?)\s*```/g;
      const matches = content.match(jsonBlockRegex);

      if (!matches) return false;

      // 检查每个JSON块是否完整且有效
      return matches.every(match => {
        const jsonContent = match.replace(/```json\s*|\s*```/g, '');
        try {
          JSON.parse(jsonContent);
          return true;
        } catch (e) {
          return false;
        }
      });
    },

    // 判断内容是否看起来已经完成
    looksLikeCompleteContent(content) {
      // 检查一些完成的标志
      const completionIndicators = [
        /Step \d+:\s*思考完成/,
        /工具.*返回的结果：.*$/,
        /```\s*$/,
        /\n\n$/,
        content.length > 1000 && !content.endsWith('...')
      ];

      return completionIndicators.some(indicator => {
        if (typeof indicator === 'boolean') return indicator;
        return indicator.test(content);
      });
    },

    // 完整内容解析
    parseCompleteContent(content) {
      let processedContent = content;

      // 步骤式输出格式优化
      processedContent = this.formatStepOutput(processedContent);

      // JSON格式解析
      processedContent = this.parseJSONContent(processedContent);

      // JSON转Markdown格式
      processedContent = this.convertJSONToMarkdown(processedContent);

      // Markdown格式解析
      processedContent = this.parseMarkdownContent(processedContent);

      return processedContent;
    },

    // 部分内容解析（安全模式）
    parsePartialContent(content) {
      let processedContent = content;

      // 只进行基础的换行符处理和步骤格式化
      processedContent = this.formatStepOutput(processedContent);

      // 对于部分内容，只进行安全的JSON解析（不转换）
      processedContent = this.parseJSONContentSafe(processedContent);

      // 基础的Markdown解析（避免表格等复杂解析）
      processedContent = this.parseMarkdownContentBasic(processedContent);

      return processedContent;
    },

    // 安全的JSON解析（不进行转换）
    parseJSONContentSafe(content) {
      // 只对明确完整的JSON代码块进行语法高亮
      content = content.replace(/```json\s*([\s\S]*?)\s*```/gi, (match, jsonContent) => {
        try {
          JSON.parse(jsonContent.trim());
          // 只有有效的JSON才进行高亮处理
          return `<div class="json-block">
            <div class="json-header">📄 JSON数据</div>
            <div class="json-content">${this.formatJSONObject(JSON.parse(jsonContent.trim()))}</div>
          </div>`;
        } catch (e) {
          // 无效JSON保持原样
          return match;
        }
      });

      return content;
    },

    // 基础的Markdown解析
    parseMarkdownContentBasic(content) {
      // 只进行基础的Markdown解析，避免复杂的表格处理
      if (/<[^>]+>/.test(content)) {
        return content; // 已包含HTML，跳过
      }

      // 只处理简单的格式
      content = content.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
      content = content.replace(/\*(.*?)\*/g, '<em>$1</em>');
      content = content.replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>');

      return content;
    },

    // 格式化步骤式输出
    formatStepOutput(content) {
      // 检查是否包含步骤式输出模式
      const stepPattern = /Step \d+:/g;
      if (!stepPattern.test(content)) {
        return content;
      }

      let formattedContent = content;

      // 1. 处理步骤标题
      formattedContent = formattedContent.replace(
        /Step (\d+):\s*([^S]*?)(?=Step \d+:|$)/g,
        (match, stepNum, stepContent) => {
          const cleanContent = stepContent.trim();

          // 如果是"思考完成 - 无需行动"，使用简化显示
          if (cleanContent.includes('思考完成 - 无需行动')) {
            return `<div class="step-item thinking">
              <div class="step-header">
                <span class="step-number">${stepNum}</span>
                <span class="step-title">思考中...</span>
              </div>
            </div>`;
          }

          // 处理工具调用步骤
          if (cleanContent.includes('工具') && cleanContent.includes('返回的结果')) {
            const toolMatch = cleanContent.match(/工具\s+(\w+)\s+返回的结果：(.+)/);
            if (toolMatch) {
              const toolName = toolMatch[1];
              const result = toolMatch[2];

              return `<div class="step-item tool-call">
                <div class="step-header">
                  <span class="step-number">${stepNum}</span>
                  <span class="step-title">🔧 调用工具: ${this.getToolDisplayName(toolName)}</span>
                </div>
                <div class="step-content">
                  ${this.formatToolResult(toolName, result)}
                </div>
              </div>`;
            }
          }

          // 其他步骤的通用处理
          return `<div class="step-item">
            <div class="step-header">
              <span class="step-number">${stepNum}</span>
              <span class="step-title">执行步骤</span>
            </div>
            <div class="step-content">${cleanContent}</div>
          </div>`;
        }
      );

      return `<div class="step-output">${formattedContent}</div>`;
    },

    // 获取工具的显示名称
    getToolDisplayName(toolName) {
      const toolNames = {
        'searchWeb': '🔍 网络搜索',
        'chatWithZhipuAI': '🤖 AI分析',
        'generatePDF': '📄 生成PDF',
        'copyLocalFile': '📁 文件操作',
        'readFile': '📖 读取文件',
        'writeFile': '✏️ 写入文件',
        'executeCommand': '⚡ 执行命令'
      };
      return toolNames[toolName] || `🔧 ${toolName}`;
    },

    // 格式化工具结果
    formatToolResult(toolName, result) {
      try {
        // 尝试解析JSON结果
        if (result.startsWith('"') && result.endsWith('"')) {
          const parsed = JSON.parse(result);

          if (toolName === 'searchWeb') {
            return this.formatSearchResults(parsed);
          } else if (toolName === 'chatWithZhipuAI') {
            return this.formatAIResponse(parsed);
          } else if (toolName === 'generatePDF' || toolName === 'copyLocalFile') {
            return this.formatFileOperation(parsed);
          }
        }

        // 如果不是JSON，直接显示
        return `<div class="tool-result">${result}</div>`;
      } catch (e) {
        return `<div class="tool-result">${result}</div>`;
      }
    },

    // 格式化搜索结果
    formatSearchResults(data) {
      if (typeof data === 'string') {
        try {
          data = JSON.parse(data);
        } catch (e) {
          return `<div class="tool-result">${data}</div>`;
        }
      }

      if (Array.isArray(data) || (data && typeof data === 'object')) {
        const results = Array.isArray(data) ? data : [data];
        let html = '<div class="search-results">';

        results.slice(0, 3).forEach((item, index) => {
          if (item.title && item.link) {
            html += `
              <div class="search-item">
                <div class="search-title">
                  <a href="${item.link}" target="_blank">${item.title}</a>
                </div>
                <div class="search-snippet">${item.snippet || ''}</div>
                <div class="search-link">${item.displayed_link || item.link}</div>
              </div>
            `;
          }
        });

        html += '</div>';
        return html;
      }

      return `<div class="tool-result">${JSON.stringify(data, null, 2)}</div>`;
    },

    // 格式化AI响应
    formatAIResponse(response) {
      // 检查是否包含表格
      if (response.includes('|') && response.includes('---')) {
        return `<div class="ai-response markdown-content">${this.parseMarkdownTable(response)}</div>`;
      }

      return `<div class="ai-response">${response}</div>`;
    },

    // 解析Markdown表格
    parseMarkdownTable(content) {
      const lines = content.split('\n');
      let html = '';
      let inTable = false;
      let tableHtml = '';

      for (let i = 0; i < lines.length; i++) {
        const line = lines[i].trim();

        if (line.includes('|') && !line.startsWith('|---')) {
          if (!inTable) {
            inTable = true;
            tableHtml = '<table class="markdown-table"><thead>';
          }

          const cells = line.split('|').map(cell => cell.trim()).filter(cell => cell);

          if (i === 0 || (i === 1 && lines[i-1].includes('|'))) {
            // 表头
            tableHtml += '<tr>';
            cells.forEach(cell => {
              tableHtml += `<th>${cell}</th>`;
            });
            tableHtml += '</tr>';

            if (i === 0 && lines[i+1] && lines[i+1].includes('---')) {
              tableHtml += '</thead><tbody>';
            }
          } else {
            // 表格内容
            tableHtml += '<tr>';
            cells.forEach(cell => {
              tableHtml += `<td>${cell}</td>`;
            });
            tableHtml += '</tr>';
          }
        } else if (inTable && !line.includes('|')) {
          // 表格结束
          tableHtml += '</tbody></table>';
          html += tableHtml;
          inTable = false;
          tableHtml = '';

          if (line) {
            html += `<p>${line}</p>`;
          }
        } else if (!inTable && line) {
          html += `<p>${line}</p>`;
        }
      }

      if (inTable) {
        tableHtml += '</tbody></table>';
        html += tableHtml;
      }

      return html;
    },

    // 格式化文件操作结果
    formatFileOperation(result) {
      if (result.includes('successfully')) {
        const pathMatch = result.match(/to:\s*(.+)$/);
        const path = pathMatch ? pathMatch[1] : '';

        return `
          <div class="file-operation success">
            <div class="operation-status">✅ 操作成功</div>
            ${path ? `<div class="file-path">📁 ${path}</div>` : ''}
          </div>
        `;
      }

      return `<div class="tool-result">${result}</div>`;
    },

    // JSON内容解析
    parseJSONContent(content) {
      // 匹配JSON代码块 ```json ... ```
      content = content.replace(/```json\s*([\s\S]*?)\s*```/gi, (match, jsonContent) => {
        try {
          const parsed = JSON.parse(jsonContent.trim());
          return `<div class="json-block">
            <div class="json-header">📄 JSON数据</div>
            <div class="json-content">${this.formatJSONObject(parsed)}</div>
          </div>`;
        } catch (e) {
          return `<div class="json-block error">
            <div class="json-header">❌ JSON格式错误</div>
            <pre class="json-raw">${jsonContent}</pre>
          </div>`;
        }
      });

      // 匹配内联JSON对象 {...} 或 [...]
      content = content.replace(/(\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\}|\[[^\[\]]*(?:\[[^\[\]]*\][^\[\]]*)*\])/g, (match) => {
        // 跳过已经在HTML标签中的内容
        if (match.includes('<') || match.includes('>')) {
          return match;
        }

        try {
          const parsed = JSON.parse(match);
          if (typeof parsed === 'object' && parsed !== null) {
            return `<span class="inline-json" title="点击展开JSON">${this.formatInlineJSON(parsed)}</span>`;
          }
        } catch (e) {
          // 不是有效JSON，保持原样
        }
        return match;
      });

      return content;
    },

    // JSON转Markdown格式
    convertJSONToMarkdown(content) {
      // 检测并转换JSON数组为Markdown表格
      content = content.replace(/```json\s*([\s\S]*?)\s*```/gi, (match, jsonContent) => {
        try {
          const parsed = JSON.parse(jsonContent.trim());

          // 如果是数组且包含对象，转换为表格
          if (Array.isArray(parsed) && parsed.length > 0 && typeof parsed[0] === 'object') {
            return this.convertArrayToMarkdownTable(parsed);
          }

          // 如果是单个对象，转换为键值对表格
          if (typeof parsed === 'object' && parsed !== null && !Array.isArray(parsed)) {
            return this.convertObjectToMarkdownTable(parsed);
          }

          // 其他情况保持JSON格式
          return match;
        } catch (e) {
          return match;
        }
      });

      // 检测内联JSON对象并提供转换选项
      content = content.replace(/(\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\})/g, (match) => {
        if (match.includes('<') || match.includes('>')) {
          return match;
        }

        try {
          const parsed = JSON.parse(match);
          if (typeof parsed === 'object' && parsed !== null) {
            // 为复杂对象添加转换按钮
            const keys = Object.keys(parsed);
            if (keys.length > 2) {
              return `<div class="json-convert-container">
                <span class="inline-json">${this.formatInlineJSON(parsed)}</span>
                <button class="convert-to-table-btn" onclick="convertJSONToTable(this, '${this.escapeForAttribute(match)}')" title="转换为表格">📊</button>
              </div>`;
            }
          }
        } catch (e) {
          // 不是有效JSON
        }
        return match;
      });

      return content;
    },

    // 将JSON数组转换为Markdown表格
    convertArrayToMarkdownTable(array) {
      if (!Array.isArray(array) || array.length === 0) {
        return '空数组';
      }

      // 获取所有可能的键
      const allKeys = new Set();
      array.forEach(item => {
        if (typeof item === 'object' && item !== null) {
          Object.keys(item).forEach(key => allKeys.add(key));
        }
      });

      const keys = Array.from(allKeys);
      if (keys.length === 0) {
        return '数组不包含对象';
      }

      // 生成表格头部
      let markdown = '<div class="json-to-markdown">\n';
      markdown += '<div class="conversion-header">📊 JSON数据表格化显示</div>\n';
      markdown += '<table class="markdown-table">\n<thead>\n<tr>\n';
      keys.forEach(key => {
        markdown += `<th>${this.escapeHtml(key)}</th>\n`;
      });
      markdown += '</tr>\n</thead>\n<tbody>\n';

      // 生成表格内容
      array.forEach(item => {
        markdown += '<tr>\n';
        keys.forEach(key => {
          const value = item && typeof item === 'object' ? item[key] : '';
          const displayValue = this.formatTableCellValue(value);
          markdown += `<td>${displayValue}</td>\n`;
        });
        markdown += '</tr>\n';
      });

      markdown += '</tbody>\n</table>\n</div>';
      return markdown;
    },

    // 将JSON对象转换为键值对表格
    convertObjectToMarkdownTable(obj) {
      if (typeof obj !== 'object' || obj === null) {
        return '无效对象';
      }

      const keys = Object.keys(obj);
      if (keys.length === 0) {
        return '空对象';
      }

      let markdown = '<div class="json-to-markdown">\n';
      markdown += '<div class="conversion-header">📋 JSON对象属性表</div>\n';
      markdown += '<table class="markdown-table">\n<thead>\n<tr>\n';
      markdown += '<th>属性</th>\n<th>值</th>\n<th>类型</th>\n';
      markdown += '</tr>\n</thead>\n<tbody>\n';

      keys.forEach(key => {
        const value = obj[key];
        const type = this.getValueType(value);
        const displayValue = this.formatTableCellValue(value);

        markdown += '<tr>\n';
        markdown += `<td><code>${this.escapeHtml(key)}</code></td>\n`;
        markdown += `<td>${displayValue}</td>\n`;
        markdown += `<td><span class="type-badge type-${type}">${type}</span></td>\n`;
        markdown += '</tr>\n';
      });

      markdown += '</tbody>\n</table>\n</div>';
      return markdown;
    },

    // 格式化表格单元格的值
    formatTableCellValue(value) {
      if (value === null) {
        return '<span class="null-value">null</span>';
      }
      if (value === undefined) {
        return '<span class="undefined-value">undefined</span>';
      }
      if (typeof value === 'boolean') {
        return `<span class="boolean-value">${value}</span>`;
      }
      if (typeof value === 'number') {
        return `<span class="number-value">${value}</span>`;
      }
      if (typeof value === 'string') {
        // 检查是否是URL
        if (this.isURL(value)) {
          return `<a href="${value}" target="_blank" class="url-link">${this.escapeHtml(value)}</a>`;
        }
        return this.escapeHtml(value);
      }
      if (typeof value === 'object') {
        if (Array.isArray(value)) {
          return `<span class="array-value">[${value.length} items]</span>`;
        }
        return `<span class="object-value">{${Object.keys(value).length} keys}</span>`;
      }
      return this.escapeHtml(String(value));
    },

    // 获取值的类型
    getValueType(value) {
      if (value === null) return 'null';
      if (Array.isArray(value)) return 'array';
      return typeof value;
    },

    // 检查是否是URL
    isURL(string) {
      try {
        new URL(string);
        return true;
      } catch (_) {
        return false;
      }
    },

    // 转义属性值用于HTML属性
    escapeForAttribute(str) {
      return str.replace(/'/g, '&#39;').replace(/"/g, '&quot;');
    },

    // Markdown内容解析
    parseMarkdownContent(content) {
      // 如果已经包含HTML标签，跳过部分Markdown解析
      const hasHTML = /<[^>]+>/.test(content);

      if (!hasHTML) {
        // 标题解析
        content = content.replace(/^### (.*$)/gm, '<h3 class="md-h3">$1</h3>');
        content = content.replace(/^## (.*$)/gm, '<h2 class="md-h2">$1</h2>');
        content = content.replace(/^# (.*$)/gm, '<h1 class="md-h1">$1</h1>');

        // 粗体和斜体
        content = content.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
        content = content.replace(/\*(.*?)\*/g, '<em>$1</em>');

        // 代码块
        content = content.replace(/```(\w+)?\s*([\s\S]*?)\s*```/g, (match, lang, code) => {
          const language = lang || 'text';
          return `<div class="code-block">
            <div class="code-header">
              <span class="code-lang">${language}</span>
              <button class="copy-code-btn" onclick="copyCode(this)" title="复制代码">📋</button>
            </div>
            <pre class="code-content"><code class="language-${language}">${this.escapeHtml(code.trim())}</code></pre>
          </div>`;
        });

        // 内联代码
        content = content.replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>');

        // 链接
        content = content.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" class="md-link">$1</a>');

        // 无序列表
        content = content.replace(/^\* (.+$)/gm, '<li class="md-li">$1</li>');
        content = content.replace(/(<li class="md-li">.*<\/li>)/s, '<ul class="md-ul">$1</ul>');

        // 有序列表
        content = content.replace(/^\d+\. (.+$)/gm, '<li class="md-oli">$1</li>');
        content = content.replace(/(<li class="md-oli">.*<\/li>)/s, '<ol class="md-ol">$1</ol>');

        // 引用块
        content = content.replace(/^> (.+$)/gm, '<blockquote class="md-blockquote">$1</blockquote>');

        // 分割线
        content = content.replace(/^---$/gm, '<hr class="md-hr">');
      }

      // 表格解析（总是执行，因为可能在HTML中包含Markdown表格）
      content = this.parseMarkdownTables(content);

      return content;
    },

    // 格式化JSON对象为可读的HTML
    formatJSONObject(obj, depth = 0) {
      const indent = '  '.repeat(depth);

      if (obj === null) return '<span class="json-null">null</span>';
      if (typeof obj === 'boolean') return `<span class="json-boolean">${obj}</span>`;
      if (typeof obj === 'number') return `<span class="json-number">${obj}</span>`;
      if (typeof obj === 'string') return `<span class="json-string">"${this.escapeHtml(obj)}"</span>`;

      if (Array.isArray(obj)) {
        if (obj.length === 0) return '<span class="json-array">[]</span>';

        let html = '<span class="json-bracket">[</span><br>';
        obj.forEach((item, index) => {
          html += `${indent}  ${this.formatJSONObject(item, depth + 1)}`;
          if (index < obj.length - 1) html += '<span class="json-comma">,</span>';
          html += '<br>';
        });
        html += `${indent}<span class="json-bracket">]</span>`;
        return html;
      }

      if (typeof obj === 'object') {
        const keys = Object.keys(obj);
        if (keys.length === 0) return '<span class="json-object">{}</span>';

        let html = '<span class="json-bracket">{</span><br>';
        keys.forEach((key, index) => {
          html += `${indent}  <span class="json-key">"${this.escapeHtml(key)}"</span><span class="json-colon">:</span> ${this.formatJSONObject(obj[key], depth + 1)}`;
          if (index < keys.length - 1) html += '<span class="json-comma">,</span>';
          html += '<br>';
        });
        html += `${indent}<span class="json-bracket">}</span>`;
        return html;
      }

      return this.escapeHtml(String(obj));
    },

    // 格式化内联JSON
    formatInlineJSON(obj) {
      if (Array.isArray(obj)) {
        return `[${obj.length} items]`;
      }
      if (typeof obj === 'object' && obj !== null) {
        const keys = Object.keys(obj);
        return `{${keys.length} keys}`;
      }
      return JSON.stringify(obj);
    },

    // 解析Markdown表格
    parseMarkdownTables(content) {
      // 匹配Markdown表格
      const tableRegex = /(\|[^\n]+\|\n\|[-:\s|]+\|\n(?:\|[^\n]+\|\n?)*)/g;

      return content.replace(tableRegex, (match) => {
        const lines = match.trim().split('\n');
        if (lines.length < 3) return match;

        const headers = lines[0].split('|').map(cell => cell.trim()).filter(cell => cell);
        const separators = lines[1].split('|').map(cell => cell.trim()).filter(cell => cell);
        const rows = lines.slice(2).map(line =>
          line.split('|').map(cell => cell.trim()).filter(cell => cell)
        );

        let html = '<table class="markdown-table"><thead><tr>';
        headers.forEach(header => {
          html += `<th>${header}</th>`;
        });
        html += '</tr></thead><tbody>';

        rows.forEach(row => {
          html += '<tr>';
          row.forEach((cell, index) => {
            if (index < headers.length) {
              html += `<td>${cell}</td>`;
            }
          });
          html += '</tr>';
        });

        html += '</tbody></table>';
        return html;
      });
    },

    // HTML转义
    escapeHtml(text) {
      const div = document.createElement('div');
      div.textContent = text;
      return div.innerHTML;
    },

    // 复制代码到剪贴板
    copyCodeToClipboard(button) {
      const codeBlock = button.closest('.code-block');
      const codeContent = codeBlock.querySelector('.code-content code');
      const text = codeContent.textContent;

      if (navigator.clipboard) {
        navigator.clipboard.writeText(text).then(() => {
          button.textContent = '✅';
          setTimeout(() => {
            button.textContent = '📋';
          }, 2000);
        }).catch(err => {
          console.error('复制失败:', err);
          this.fallbackCopyTextToClipboard(text, button);
        });
      } else {
        this.fallbackCopyTextToClipboard(text, button);
      }
    },

    // 备用复制方法
    fallbackCopyTextToClipboard(text, button) {
      const textArea = document.createElement('textarea');
      textArea.value = text;
      textArea.style.position = 'fixed';
      textArea.style.left = '-999999px';
      textArea.style.top = '-999999px';
      document.body.appendChild(textArea);
      textArea.focus();
      textArea.select();

      try {
        const successful = document.execCommand('copy');
        if (successful) {
          button.textContent = '✅';
          setTimeout(() => {
            button.textContent = '📋';
          }, 2000);
        }
      } catch (err) {
        console.error('备用复制方法失败:', err);
      }

      document.body.removeChild(textArea);
    },

    // 全局JSON转表格功能
    convertJSONToTableGlobal(button, jsonString) {
      try {
        const parsed = JSON.parse(jsonString);
        let tableHTML = '';

        if (Array.isArray(parsed)) {
          tableHTML = this.convertArrayToMarkdownTable(parsed);
        } else if (typeof parsed === 'object' && parsed !== null) {
          tableHTML = this.convertObjectToMarkdownTable(parsed);
        }

        if (tableHTML) {
          // 替换按钮所在的容器
          const container = button.closest('.json-convert-container');
          if (container) {
            container.outerHTML = tableHTML;
          }
        }
      } catch (e) {
        console.error('JSON转换失败:', e);
        button.textContent = '❌';
        setTimeout(() => {
          button.textContent = '📊';
        }, 2000);
      }
    }
  },
  watch: {
    content: {
      handler(newContent, oldContent) {
        // 检测流式输出是否完成
        if (newContent && oldContent && newContent.length > oldContent.length) {
          // 内容在增长，可能是流式输出
          this.isStreamComplete = false;

          // 设置一个定时器来检测流式输出是否完成
          if (this.streamCompleteTimer) {
            clearTimeout(this.streamCompleteTimer);
          }

          this.streamCompleteTimer = setTimeout(() => {
            this.isStreamComplete = true;
            // 流式输出完成，进行最终的完整解析
            this.$forceUpdate();
          }, 500); // 500ms没有新内容则认为完成
        }
      },
      immediate: true
    }
  },
  mounted() {
    // 添加全局复制代码功能
    if (!window.copyCode) {
      window.copyCode = this.copyCodeToClipboard;
    }

    // 添加全局JSON转表格功能
    if (!window.convertJSONToTable) {
      window.convertJSONToTable = this.convertJSONToTableGlobal;
    }
  },
  beforeDestroy() {
    // 清理定时器
    if (this.parseTimeout) {
      clearTimeout(this.parseTimeout);
    }
    if (this.streamCompleteTimer) {
      clearTimeout(this.streamCompleteTimer);
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

/* 步骤式输出样式 */
.step-output {
  margin: 10px 0;
}

.step-item {
  margin-bottom: 12px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.step-item.thinking {
  background-color: #f8f9fa;
  border-left: 4px solid #6c757d;
}

.step-item.tool-call {
  background-color: #fff;
  border-left: 4px solid #007bff;
}

.step-header {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  background-color: rgba(0, 0, 0, 0.05);
  font-weight: 500;
}

.step-item.thinking .step-header {
  background-color: rgba(108, 117, 125, 0.1);
}

.step-item.tool-call .step-header {
  background-color: rgba(0, 123, 255, 0.1);
}

.step-number {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  background-color: #007bff;
  color: white;
  border-radius: 50%;
  font-size: 12px;
  font-weight: bold;
  margin-right: 8px;
}

.step-item.thinking .step-number {
  background-color: #6c757d;
}

.step-title {
  font-size: 14px;
  color: #333;
}

.step-content {
  padding: 12px;
  background-color: #fff;
}

/* 搜索结果样式 */
.search-results {
  margin-top: 8px;
}

.search-item {
  margin-bottom: 12px;
  padding: 10px;
  background-color: #f8f9fa;
  border-radius: 6px;
  border-left: 3px solid #28a745;
}

.search-title a {
  color: #1a73e8;
  text-decoration: none;
  font-weight: 500;
  font-size: 14px;
}

.search-title a:hover {
  text-decoration: underline;
}

.search-snippet {
  margin: 6px 0;
  color: #5f6368;
  font-size: 13px;
  line-height: 1.4;
}

.search-link {
  color: #006621;
  font-size: 12px;
}

/* AI响应样式 */
.ai-response {
  background-color: #f0f8ff;
  padding: 12px;
  border-radius: 6px;
  border-left: 3px solid #17a2b8;
}

/* Markdown表格样式 */
.markdown-table {
  width: 100%;
  border-collapse: collapse;
  margin: 10px 0;
  font-size: 13px;
}

.markdown-table th,
.markdown-table td {
  border: 1px solid #ddd;
  padding: 8px 12px;
  text-align: left;
}

.markdown-table th {
  background-color: #f8f9fa;
  font-weight: 600;
  color: #333;
}

.markdown-table tr:nth-child(even) {
  background-color: #f9f9f9;
}

.markdown-table tr:hover {
  background-color: #f5f5f5;
}

/* 文件操作样式 */
.file-operation {
  padding: 10px;
  border-radius: 6px;
  margin-top: 8px;
}

.file-operation.success {
  background-color: #d4edda;
  border-left: 3px solid #28a745;
}

.operation-status {
  font-weight: 500;
  color: #155724;
  margin-bottom: 4px;
}

.file-path {
  font-family: 'Courier New', monospace;
  font-size: 12px;
  color: #495057;
  background-color: rgba(0, 0, 0, 0.05);
  padding: 4px 8px;
  border-radius: 4px;
  word-break: break-all;
}

/* 工具结果通用样式 */
.tool-result {
  background-color: #f8f9fa;
  padding: 10px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  color: #495057;
  white-space: pre-wrap;
  word-break: break-word;
}

/* JSON内容样式 */
.json-block {
  margin: 10px 0;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #e9ecef;
}

.json-block.error {
  border-color: #dc3545;
}

.json-header {
  background-color: #f8f9fa;
  padding: 8px 12px;
  font-weight: 500;
  font-size: 13px;
  border-bottom: 1px solid #e9ecef;
}

.json-block.error .json-header {
  background-color: #f8d7da;
  color: #721c24;
}

.json-content {
  padding: 12px;
  background-color: #fff;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.4;
  overflow-x: auto;
}

.json-raw {
  margin: 0;
  padding: 12px;
  background-color: #fff;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-word;
}

/* JSON语法高亮 */
.json-key { color: #0066cc; font-weight: 500; }
.json-string { color: #009900; }
.json-number { color: #cc6600; }
.json-boolean { color: #cc0066; font-weight: 500; }
.json-null { color: #999999; font-style: italic; }
.json-bracket { color: #333333; font-weight: bold; }
.json-comma { color: #666666; }
.json-colon { color: #666666; margin: 0 4px; }

/* 内联JSON样式 */
.inline-json {
  background-color: #f8f9fa;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  font-size: 11px;
  color: #495057;
  cursor: pointer;
  border: 1px solid #e9ecef;
}

.inline-json:hover {
  background-color: #e9ecef;
}

/* Markdown样式 */
.md-h1 {
  font-size: 1.5em;
  font-weight: bold;
  margin: 16px 0 8px 0;
  color: #333;
  border-bottom: 2px solid #eee;
  padding-bottom: 4px;
}

.md-h2 {
  font-size: 1.3em;
  font-weight: bold;
  margin: 14px 0 6px 0;
  color: #333;
  border-bottom: 1px solid #eee;
  padding-bottom: 2px;
}

.md-h3 {
  font-size: 1.1em;
  font-weight: bold;
  margin: 12px 0 4px 0;
  color: #333;
}

.md-ul, .md-ol {
  margin: 8px 0;
  padding-left: 20px;
}

.md-li, .md-oli {
  margin: 4px 0;
  line-height: 1.4;
}

.md-blockquote {
  margin: 8px 0;
  padding: 8px 12px;
  background-color: #f8f9fa;
  border-left: 4px solid #007bff;
  color: #495057;
  font-style: italic;
}

.md-hr {
  margin: 16px 0;
  border: none;
  border-top: 1px solid #eee;
}

.md-link {
  color: #007bff;
  text-decoration: none;
}

.md-link:hover {
  text-decoration: underline;
}

/* 代码块样式 */
.code-block {
  margin: 10px 0;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #e9ecef;
}

.code-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #f8f9fa;
  padding: 6px 12px;
  border-bottom: 1px solid #e9ecef;
}

.code-lang {
  font-size: 11px;
  color: #666;
  font-weight: 500;
  text-transform: uppercase;
}

.copy-code-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 12px;
  color: #666;
  padding: 2px 4px;
  border-radius: 3px;
  transition: background-color 0.2s;
}

.copy-code-btn:hover {
  background-color: #e9ecef;
}

.code-content {
  margin: 0;
  padding: 12px;
  background-color: #fff;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.4;
  overflow-x: auto;
  white-space: pre;
}

.inline-code {
  background-color: #f8f9fa;
  padding: 2px 4px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  font-size: 0.9em;
  color: #e83e8c;
}

/* JSON转Markdown样式 */
.json-to-markdown {
  margin: 15px 0;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  overflow: hidden;
  background-color: #fff;
}

.conversion-header {
  background-color: #f8f9fa;
  padding: 8px 12px;
  font-weight: 500;
  font-size: 13px;
  color: #495057;
  border-bottom: 1px solid #e9ecef;
}

.json-convert-container {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.convert-to-table-btn {
  background: none;
  border: 1px solid #007bff;
  color: #007bff;
  padding: 2px 6px;
  border-radius: 3px;
  cursor: pointer;
  font-size: 11px;
  transition: all 0.2s;
}

.convert-to-table-btn:hover {
  background-color: #007bff;
  color: white;
}

/* 表格单元格值类型样式 */
.null-value {
  color: #999999;
  font-style: italic;
}

.undefined-value {
  color: #999999;
  font-style: italic;
}

.boolean-value {
  color: #cc0066;
  font-weight: 500;
}

.number-value {
  color: #cc6600;
  font-weight: 500;
}

.array-value {
  color: #6f42c1;
  font-style: italic;
}

.object-value {
  color: #6f42c1;
  font-style: italic;
}

.url-link {
  color: #007bff;
  text-decoration: none;
  word-break: break-all;
}

.url-link:hover {
  text-decoration: underline;
}

/* 类型标签样式 */
.type-badge {
  display: inline-block;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 10px;
  font-weight: 500;
  text-transform: uppercase;
}

.type-string {
  background-color: #d4edda;
  color: #155724;
}

.type-number {
  background-color: #fff3cd;
  color: #856404;
}

.type-boolean {
  background-color: #f8d7da;
  color: #721c24;
}

.type-object {
  background-color: #d1ecf1;
  color: #0c5460;
}

.type-array {
  background-color: #e2e3e5;
  color: #383d41;
}

.type-null {
  background-color: #f8f9fa;
  color: #6c757d;
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