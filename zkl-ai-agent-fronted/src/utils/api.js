import axios from 'axios';

const API_URL = '/api';

export const api = {
  // 调用疾控监督专家SSE接口
  chatWithInspectorSSE(message, chatId, onMessage, onError, onComplete) {
    const eventSource = new EventSource(`${API_URL}/ai/inspector_app/chat/sse?message=${encodeURIComponent(message)}&chatId=${encodeURIComponent(chatId)}`);
    
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
    const eventSource = new EventSource(`${API_URL}/ai/manus/chat?message=${encodeURIComponent(message)}`);
    
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