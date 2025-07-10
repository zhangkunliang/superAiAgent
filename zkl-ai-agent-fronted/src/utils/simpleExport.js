/**
 * 简化的导出工具 - 专注于浏览器兼容性和中文支持
 */

/**
 * 导出为富文本 HTML 文件
 * @param {Array} messages - 聊天消息数组
 * @param {string} chatTitle - 聊天标题
 * @param {string} chatId - 聊天ID
 */
export function exportToHTML(messages, chatTitle = '聊天记录', chatId = '') {
  try {
    const htmlContent = generateHTMLContent(messages, chatTitle, chatId);
    
    const blob = new Blob([htmlContent], { type: 'text/html;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${chatTitle}_${new Date().toISOString().slice(0, 10)}.html`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);

    return true;
  } catch (error) {
    console.error('导出HTML文件失败:', error);
    throw new Error('导出HTML文件失败: ' + error.message);
  }
}

/**
 * 导出为纯文本文件
 * @param {Array} messages - 聊天消息数组
 * @param {string} chatTitle - 聊天标题
 * @param {string} chatId - 聊天ID
 */
export function exportToText(messages, chatTitle = '聊天记录', chatId = '') {
  try {
    let content = `${chatTitle}\n`;
    content += '='.repeat(chatTitle.length) + '\n\n';
    
    if (chatId) {
      content += `会话ID: ${chatId}\n`;
    }
    content += `导出时间: ${new Date().toLocaleString('zh-CN')}\n\n`;
    content += '-'.repeat(50) + '\n\n';

    messages.forEach((message, index) => {
      const senderName = message.role === 'user' ? '用户' : 'AI助手';
      const timestamp = new Date(message.timestamp).toLocaleString('zh-CN');
      
      content += `${senderName} (${timestamp})\n`;
      content += '-'.repeat(30) + '\n';
      content += cleanHtmlContent(message.content) + '\n\n';
      
      if (index < messages.length - 1) {
        content += '\n';
      }
    });

    const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${chatTitle}_${new Date().toISOString().slice(0, 10)}.txt`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);

    return true;
  } catch (error) {
    console.error('导出文本文件失败:', error);
    throw new Error('导出文本文件失败: ' + error.message);
  }
}

/**
 * 导出为 Markdown 文件
 * @param {Array} messages - 聊天消息数组
 * @param {string} chatTitle - 聊天标题
 * @param {string} chatId - 聊天ID
 */
export function exportToMarkdown(messages, chatTitle = '聊天记录', chatId = '') {
  try {
    let content = `# ${chatTitle}\n\n`;
    
    if (chatId) {
      content += `**会话ID:** ${chatId}\n\n`;
    }
    content += `**导出时间:** ${new Date().toLocaleString('zh-CN')}\n\n`;
    content += '---\n\n';

    messages.forEach((message, index) => {
      const senderName = message.role === 'user' ? '👤 用户' : '🤖 AI助手';
      const timestamp = new Date(message.timestamp).toLocaleString('zh-CN');
      
      content += `## ${senderName}\n\n`;
      content += `*${timestamp}*\n\n`;
      
      const cleanContent = cleanHtmlContent(message.content);
      content += cleanContent + '\n\n';
      
      if (index < messages.length - 1) {
        content += '---\n\n';
      }
    });

    const blob = new Blob([content], { type: 'text/markdown;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${chatTitle}_${new Date().toISOString().slice(0, 10)}.md`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);

    return true;
  } catch (error) {
    console.error('导出Markdown文件失败:', error);
    throw new Error('导出Markdown文件失败: ' + error.message);
  }
}

/**
 * 生成 HTML 内容
 */
function generateHTMLContent(messages, chatTitle, chatId) {
  const styles = `
    <style>
      body {
        font-family: 'Microsoft YaHei', 'SimSun', Arial, sans-serif;
        max-width: 800px;
        margin: 0 auto;
        padding: 20px;
        line-height: 1.6;
        color: #333;
        background-color: #f5f5f5;
      }
      .header {
        background: white;
        padding: 20px;
        border-radius: 8px;
        margin-bottom: 20px;
        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
      }
      .header h1 {
        margin: 0 0 10px 0;
        color: #2c3e50;
        border-bottom: 3px solid #3498db;
        padding-bottom: 10px;
      }
      .header .meta {
        color: #7f8c8d;
        font-size: 14px;
      }
      .message {
        background: white;
        margin-bottom: 15px;
        border-radius: 8px;
        overflow: hidden;
        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
      }
      .message-header {
        padding: 12px 20px;
        font-weight: bold;
        font-size: 14px;
      }
      .message-user .message-header {
        background-color: #3498db;
        color: white;
      }
      .message-assistant .message-header {
        background-color: #2ecc71;
        color: white;
      }
      .message-content {
        padding: 15px 20px;
        white-space: pre-wrap;
        word-wrap: break-word;
      }
      .timestamp {
        float: right;
        font-weight: normal;
        opacity: 0.8;
      }
      @media print {
        body { background-color: white; }
        .message { box-shadow: none; border: 1px solid #ddd; }
      }
    </style>
  `;

  let htmlContent = `
    <!DOCTYPE html>
    <html lang="zh-CN">
    <head>
      <meta charset="UTF-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <title>${chatTitle}</title>
      ${styles}
    </head>
    <body>
      <div class="header">
        <h1>${chatTitle}</h1>
        <div class="meta">
          ${chatId ? `会话ID: ${chatId}<br>` : ''}
          导出时间: ${new Date().toLocaleString('zh-CN')}
        </div>
      </div>
  `;

  messages.forEach(message => {
    const senderName = message.role === 'user' ? '用户' : 'AI助手';
    const timestamp = new Date(message.timestamp).toLocaleString('zh-CN');
    const content = cleanHtmlContent(message.content).replace(/\n/g, '<br>');
    
    htmlContent += `
      <div class="message message-${message.role}">
        <div class="message-header">
          ${senderName}
          <span class="timestamp">${timestamp}</span>
        </div>
        <div class="message-content">${content}</div>
      </div>
    `;
  });

  htmlContent += `
    </body>
    </html>
  `;

  return htmlContent;
}

/**
 * 清理HTML内容，提取纯文本
 * @param {string} content - HTML内容
 * @returns {string} 清理后的文本
 */
function cleanHtmlContent(content) {
  if (!content) return '';
  
  let cleanContent = content;
  
  // 首先尝试解码JSON转义字符
  try {
    if (cleanContent.includes('\\n') || cleanContent.includes('\\r')) {
      const tempJson = '"' + cleanContent.replace(/"/g, '\\"') + '"';
      const decoded = JSON.parse(tempJson);
      cleanContent = decoded;
    }
  } catch (e) {
    // 如果JSON解码失败，继续手动处理
  }
  
  // 替换HTML标签和转义字符
  cleanContent = cleanContent
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<\/p>/gi, '\n')
    .replace(/<p[^>]*>/gi, '')
    .replace(/<strong[^>]*>(.*?)<\/strong>/gi, '$1')
    .replace(/<em[^>]*>(.*?)<\/em>/gi, '$1')
    .replace(/<code[^>]*>(.*?)<\/code>/gi, '$1')
    .replace(/<[^>]*>/g, '')
    .replace(/&nbsp;/g, ' ')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&amp;/g, '&')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/\\n/g, '\n')
    .replace(/\\r/g, '\n')
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n');
  
  // 清理多余的空行
  cleanContent = cleanContent
    .split('\n')
    .map(line => line.trim())
    .filter((line, index, array) => {
      return line || (index > 0 && array[index - 1]);
    })
    .join('\n')
    .trim();
  
  return cleanContent;
}
