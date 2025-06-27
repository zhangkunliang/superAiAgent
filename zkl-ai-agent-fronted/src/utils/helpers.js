/**
 * 生成唯一的聊天室ID
 * @returns {string} UUID格式的聊天室ID
 */
export function generateChatId() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0;
    const v = c === 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

/**
 * 格式化日期时间
 * @param {string} isoString - ISO格式的日期字符串
 * @returns {string} 格式化后的日期时间字符串
 */
export function formatDateTime(isoString) {
  const date = new Date(isoString);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
}

/**
 * 解析Markdown格式文本
 * @param {string} text - 原始文本
 * @returns {string} 解析后的HTML
 */
export function parseMarkdown(text) {
  // 这里简单实现，实际项目中应该使用marked或其他库
  if (!text) return '';
  
  // 换行符转换为<br>
  let html = text.replace(/\n/g, '<br>');
  
  // 代码块
  html = html.replace(/```(\w*)([\s\S]*?)```/g, '<pre><code class="$1">$2</code></pre>');
  
  // 行内代码
  html = html.replace(/`([^`]+)`/g, '<code>$1</code>');
  
  // 粗体
  html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
  
  // 斜体
  html = html.replace(/\*([^*]+)\*/g, '<em>$1</em>');
  
  // 标题
  html = html.replace(/### (.*?)(?:\n|$)/g, '<h3>$1</h3>');
  html = html.replace(/## (.*?)(?:\n|$)/g, '<h2>$1</h2>');
  html = html.replace(/# (.*?)(?:\n|$)/g, '<h1>$1</h1>');
  
  // 列表
  html = html.replace(/- (.*?)(?:\n|$)/g, '<li>$1</li>');
  
  return html;
} 