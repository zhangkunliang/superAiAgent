// 动态导入依赖库，避免构建时错误
let docxModule, jsPDFModule, html2canvasModule;

// 异步加载依赖
async function loadDependencies() {
  try {
    if (!docxModule) {
      docxModule = await import('docx');
    }
    if (!jsPDFModule) {
      jsPDFModule = await import('jspdf');
    }
    if (!html2canvasModule) {
      html2canvasModule = await import('html2canvas');
    }
  } catch (error) {
    console.warn('Some export libraries are not available:', error.message);
  }
}

/**
 * 将聊天消息导出为 Word 文档
 * @param {Array} messages - 聊天消息数组
 * @param {string} chatTitle - 聊天标题
 * @param {string} chatId - 聊天ID
 */
/**
 * 导出为纯文本文件（备用方案）
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

    // 创建并下载文件
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

export async function exportToWord(messages, chatTitle = '聊天记录', chatId = '') {
  try {
    // 尝试加载依赖
    await loadDependencies();

    if (!docxModule) {
      console.warn('docx library not available, falling back to text export');
      return exportToText(messages, chatTitle, chatId);
    }

    const { Document, Packer, Paragraph, TextRun, HeadingLevel } = docxModule;

    // 创建文档段落数组
    const children = [];

    // 添加标题
    children.push(
      new Paragraph({
        text: chatTitle,
        heading: HeadingLevel.HEADING_1,
        spacing: { after: 300 }
      })
    );

    // 添加会话信息
    if (chatId) {
      children.push(
        new Paragraph({
          children: [
            new TextRun({
              text: `会话ID: ${chatId}`,
              size: 20,
              color: '666666'
            })
          ],
          spacing: { after: 200 }
        })
      );
    }

    children.push(
      new Paragraph({
        children: [
          new TextRun({
            text: `导出时间: ${new Date().toLocaleString('zh-CN')}`,
            size: 20,
            color: '666666'
          })
        ],
        spacing: { after: 400 }
      })
    );

    // 处理每条消息
    messages.forEach((message, index) => {
      // 添加消息头部
      const senderName = message.role === 'user' ? '用户' : 'AI助手';
      const timestamp = new Date(message.timestamp).toLocaleString('zh-CN');
      
      children.push(
        new Paragraph({
          children: [
            new TextRun({
              text: `${senderName} (${timestamp})`,
              bold: true,
              size: 24,
              color: message.role === 'user' ? '0066CC' : '009900'
            })
          ],
          spacing: { before: 200, after: 100 }
        })
      );

      // 处理消息内容
      const content = cleanHtmlContent(message.content);
      const contentLines = content.split('\n').filter(line => line.trim());

      contentLines.forEach(line => {
        children.push(
          new Paragraph({
            children: [
              new TextRun({
                text: line.trim(),
                size: 22
              })
            ],
            spacing: { after: 100 }
          })
        );
      });

      // 添加分隔线（除了最后一条消息）
      if (index < messages.length - 1) {
        children.push(
          new Paragraph({
            text: '─'.repeat(50),
            spacing: { before: 200, after: 200 }
          })
        );
      }
    });

    // 创建文档
    const doc = new Document({
      sections: [{
        properties: {},
        children: children
      }]
    });

    // 生成并下载文件 - 使用 toBlob 而不是 toBuffer
    try {
      const blob = await Packer.toBlob(doc);

      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `${chatTitle}_${new Date().toISOString().slice(0, 10)}.docx`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    } catch (blobError) {
      console.warn('Blob generation failed, falling back to text export:', blobError);
      return exportToText(messages, chatTitle, chatId);
    }

    return true;
  } catch (error) {
    console.error('导出Word文档失败:', error);
    throw new Error('导出Word文档失败: ' + error.message);
  }
}

/**
 * 将聊天消息导出为 PDF 文档
 * @param {Array} messages - 聊天消息数组
 * @param {string} chatTitle - 聊天标题
 * @param {string} chatId - 聊天ID
 */
export async function exportToPDF(messages, chatTitle = '聊天记录', chatId = '') {
  try {
    // 尝试加载依赖
    await loadDependencies();

    if (!jsPDFModule) {
      console.warn('jsPDF library not available, falling back to text export');
      return exportToText(messages, chatTitle, chatId);
    }

    const jsPDF = jsPDFModule.default || jsPDFModule;

    // 创建 PDF 文档
    const pdf = new jsPDF('p', 'mm', 'a4');

    // 由于 jsPDF 在浏览器中对中文支持有限，我们使用 HTML 转 Canvas 的方式
    return await exportToPDFViaCanvas(messages, chatTitle, chatId, pdf);
  } catch (error) {
    console.error('导出PDF文档失败:', error);
    // 如果 PDF 导出失败，降级为文本导出
    console.warn('PDF export failed, falling back to text export');
    return exportToText(messages, chatTitle, chatId);
  }
}

/**
 * 通过 Canvas 方式导出 PDF（支持中文）
 */
async function exportToPDFViaCanvas(messages, chatTitle, chatId, pdf) {
  try {
    // 创建一个临时的 HTML 容器来渲染内容
    const container = document.createElement('div');
    container.style.cssText = `
      position: absolute;
      left: -9999px;
      top: -9999px;
      width: 800px;
      background: white;
      padding: 20px;
      font-family: 'Microsoft YaHei', 'SimSun', sans-serif;
      font-size: 14px;
      line-height: 1.6;
      color: #333;
    `;

    // 构建 HTML 内容
    let htmlContent = `
      <div style="margin-bottom: 30px;">
        <h1 style="color: #333; border-bottom: 2px solid #007bff; padding-bottom: 10px;">${chatTitle}</h1>
        ${chatId ? `<p style="color: #666; margin: 10px 0;">会话ID: ${chatId}</p>` : ''}
        <p style="color: #666; margin: 10px 0;">导出时间: ${new Date().toLocaleString('zh-CN')}</p>
      </div>
    `;

    messages.forEach((message, index) => {
      const senderName = message.role === 'user' ? '用户' : 'AI助手';
      const timestamp = new Date(message.timestamp).toLocaleString('zh-CN');
      const content = cleanHtmlContent(message.content);
      const bgColor = message.role === 'user' ? '#e3f2fd' : '#f5f5f5';
      const textColor = message.role === 'user' ? '#1976d2' : '#333';

      htmlContent += `
        <div style="margin-bottom: 20px; padding: 15px; background-color: ${bgColor}; border-radius: 8px; border-left: 4px solid ${message.role === 'user' ? '#2196f3' : '#4caf50'};">
          <div style="font-weight: bold; color: ${textColor}; margin-bottom: 8px;">
            ${senderName} (${timestamp})
          </div>
          <div style="white-space: pre-wrap; word-wrap: break-word;">
            ${content.replace(/\n/g, '<br>')}
          </div>
        </div>
      `;
    });

    container.innerHTML = htmlContent;
    document.body.appendChild(container);

    // 等待字体加载
    await new Promise(resolve => setTimeout(resolve, 100));

    // 使用 html2canvas 转换为图片
    if (!html2canvasModule) {
      await loadDependencies();
    }

    if (!html2canvasModule) {
      throw new Error('html2canvas not available');
    }

    const html2canvas = html2canvasModule.default || html2canvasModule;

    const canvas = await html2canvas(container, {
      scale: 2,
      useCORS: true,
      allowTaint: true,
      backgroundColor: '#ffffff',
      width: 800,
      height: container.scrollHeight
    });

    // 清理临时容器
    document.body.removeChild(container);

    // 将 canvas 添加到 PDF
    const imgData = canvas.toDataURL('image/png');
    const imgWidth = 190; // PDF 页面宽度 (mm)
    const imgHeight = (canvas.height * imgWidth) / canvas.width;

    let yPosition = 10;
    const pageHeight = 280; // PDF 页面高度 (mm)

    // 如果内容高度超过页面高度，需要分页
    if (imgHeight > pageHeight) {
      // 分页处理
      const totalPages = Math.ceil(imgHeight / pageHeight);

      for (let i = 0; i < totalPages; i++) {
        if (i > 0) {
          pdf.addPage();
        }

        const sourceY = i * pageHeight * (canvas.height / imgHeight);
        const sourceHeight = Math.min(pageHeight * (canvas.height / imgHeight), canvas.height - sourceY);

        // 创建临时 canvas 来裁剪图片
        const tempCanvas = document.createElement('canvas');
        const tempCtx = tempCanvas.getContext('2d');
        tempCanvas.width = canvas.width;
        tempCanvas.height = sourceHeight;

        tempCtx.drawImage(canvas, 0, sourceY, canvas.width, sourceHeight, 0, 0, canvas.width, sourceHeight);

        const pageImgData = tempCanvas.toDataURL('image/png');
        const pageImgHeight = (sourceHeight * imgWidth) / canvas.width;

        pdf.addImage(pageImgData, 'PNG', 10, yPosition, imgWidth, pageImgHeight);
      }
    } else {
      // 单页处理
      pdf.addImage(imgData, 'PNG', 10, yPosition, imgWidth, imgHeight);
    }

    // 下载文件
    pdf.save(`${chatTitle}_${new Date().toISOString().slice(0, 10)}.pdf`);
    return true;

  } catch (error) {
    console.error('Canvas PDF export failed:', error);
    throw error;
  }
}

/**
 * 简化的 PDF 导出（纯文本，支持中文）
 */
async function exportToPDFSimple(messages, chatTitle, chatId) {
  try {
    const jsPDF = jsPDFModule.default || jsPDFModule;
    const pdf = new jsPDF('p', 'mm', 'a4');

    // 由于中文字体问题，直接降级为文本导出
    console.warn('Using simple PDF export, falling back to text export for better Chinese support');
    return exportToText(messages, chatTitle, chatId);

  } catch (error) {
    console.error('简化PDF导出失败:', error);
    throw new Error('简化PDF导出失败: ' + error.message);
  }
}

/**
 * 使用 html2canvas 将聊天界面导出为 PDF（备用方案）
 * @param {HTMLElement} element - 要导出的DOM元素
 * @param {string} filename - 文件名
 */
export async function exportElementToPDF(element, filename = 'chat_export') {
  try {
    // 尝试加载依赖
    await loadDependencies();

    if (!html2canvasModule || !jsPDFModule) {
      throw new Error('Required libraries not available');
    }

    const html2canvas = html2canvasModule.default || html2canvasModule;
    const jsPDF = jsPDFModule.default || jsPDFModule;

    const canvas = await html2canvas(element, {
      scale: 2,
      useCORS: true,
      allowTaint: true,
      backgroundColor: '#ffffff'
    });

    const imgData = canvas.toDataURL('image/png');
    const pdf = new jsPDF('p', 'mm', 'a4');
    
    const imgWidth = 210; // A4 width in mm
    const pageHeight = 295; // A4 height in mm
    const imgHeight = (canvas.height * imgWidth) / canvas.width;
    let heightLeft = imgHeight;
    let position = 0;

    // 添加第一页
    pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight);
    heightLeft -= pageHeight;

    // 如果内容超过一页，添加更多页面
    while (heightLeft >= 0) {
      position = heightLeft - imgHeight;
      pdf.addPage();
      pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight);
      heightLeft -= pageHeight;
    }

    pdf.save(`${filename}_${new Date().toISOString().slice(0, 10)}.pdf`);
    return true;
  } catch (error) {
    console.error('导出PDF失败:', error);
    throw new Error('导出PDF失败: ' + error.message);
  }
}

/**
 * 清理HTML内容，提取纯文本
 * @param {string} htmlContent - HTML内容
 * @returns {string} 清理后的文本
 */
function cleanHtmlContent(content) {
  if (!content) return '';
  
  // 处理转义的换行符
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
      // 保留非空行，以及不连续的空行
      return line || (index > 0 && array[index - 1]);
    })
    .join('\n')
    .trim();
  
  return cleanContent;
}
