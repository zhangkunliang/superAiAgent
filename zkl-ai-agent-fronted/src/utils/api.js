import axios from 'axios';

const API_BASE_URL = import.meta.env.PROD 
  ? '/api' // 生产环境使用相对路径，适用于前后端部署在同一域名下
  : 'http://localhost:8123/api'; // 开发环境指向本地后端服务

export const api = {
  // 调用疾控监督专家SSE接口
  chatWithInspectorSSE(message, chatId, onMessage, onError, onComplete) {
    const eventSource = new EventSource(`${API_BASE_URL}/ai/inspector_app/chat/sse?message=${encodeURIComponent(message)}&chatId=${encodeURIComponent(chatId)}`);
    
    eventSource.onmessage = (event) => {
      onMessage(event.data);
    };
    
    eventSource.onerror = (error) => {
      eventSource.close();
      onError(error);
    };
    
    // 添加一个完成方法
    const complete = () => {
      eventSource.close();
      if (onComplete) onComplete();
    };
    
    return {
      close: complete
    };
  },
  
  // 调用超级智能体SSE接口
  chatWithManusSSE(message, onMessage, onError, onComplete) {
    const eventSource = new EventSource(`${API_BASE_URL}/ai/manus/chat?message=${encodeURIComponent(message)}`);
    
    eventSource.onmessage = (event) => {
      onMessage(event.data);
    };
    
    eventSource.onerror = (error) => {
      eventSource.close();
      onError(error);
    };
    
    // 添加一个完成方法
    const complete = () => {
      eventSource.close();
      if (onComplete) onComplete();
    };
    
    return {
      close: complete
    };
  }
};

export default api; 