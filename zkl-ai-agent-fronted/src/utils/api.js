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
  },

  // 调用超级智能体多模态SSE接口
  chatWithManusMultiModalSSE(message, imageFile, onMessage, onError, onComplete) {
    return new Promise((resolve, reject) => {
      // 创建FormData
      const formData = new FormData();
      formData.append('message', message);
      if (imageFile) {
        formData.append('image', imageFile);
      }

      // 使用fetch发送POST请求，因为EventSource不支持POST
      fetch(`${API_BASE_URL}/ai/manus/chat/multimodal`, {
        method: 'POST',
        body: formData,
        headers: {
          'Accept': 'text/event-stream',
        }
      })
      .then(response => {
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();

        const readStream = () => {
          reader.read().then(({ done, value }) => {
            if (done) {
              if (onComplete) onComplete();
              return;
            }

            // 解析SSE数据
            const chunk = decoder.decode(value, { stream: true });
            const lines = chunk.split('\n');

            for (const line of lines) {
              if (line.startsWith('data: ')) {
                const data = line.slice(6); // 移除 'data: ' 前缀
                if (data.trim() && data !== '[DONE]') {
                  onMessage(data);
                }
              }
            }

            readStream(); // 继续读取
          }).catch(error => {
            if (onError) onError(error);
            reject(error);
          });
        };

        readStream();

        // 返回控制对象
        const controller = {
          close: () => {
            reader.cancel();
            if (onComplete) onComplete();
          }
        };

        resolve(controller);
      })
      .catch(error => {
        if (onError) onError(error);
        reject(error);
      });
    });
  },

  // 调用疾控监督专家多模态SSE接口（如果需要的话）
  chatWithInspectorMultiModalSSE(message, chatId, imageFile, onMessage, onError, onComplete) {
    return new Promise((resolve, reject) => {
      // 创建FormData
      const formData = new FormData();
      formData.append('message', message);
      formData.append('chatId', chatId);
      if (imageFile) {
        formData.append('image', imageFile);
      }

      // 使用fetch发送POST请求
      fetch(`${API_BASE_URL}/ai/inspector_app/chat/multimodal`, {
        method: 'POST',
        body: formData,
        headers: {
          'Accept': 'text/event-stream',
        }
      })
      .then(response => {
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();

        const readStream = () => {
          reader.read().then(({ done, value }) => {
            if (done) {
              if (onComplete) onComplete();
              return;
            }

            // 解析SSE数据
            const chunk = decoder.decode(value, { stream: true });
            const lines = chunk.split('\n');

            for (const line of lines) {
              if (line.startsWith('data: ')) {
                const data = line.slice(6); // 移除 'data: ' 前缀
                if (data.trim() && data !== '[DONE]') {
                  onMessage(data);
                }
              }
            }

            readStream(); // 继续读取
          }).catch(error => {
            if (onError) onError(error);
            reject(error);
          });
        };

        readStream();

        // 返回控制对象
        const controller = {
          close: () => {
            reader.cancel();
            if (onComplete) onComplete();
          }
        };

        resolve(controller);
      })
      .catch(error => {
        if (onError) onError(error);
        reject(error);
      });
    });
  },

  // 认证相关API
  auth: {
    // 获取登录验证码
    async getCaptcha() {
      try {
        const response = await axios.get(`${API_BASE_URL}/auth/captcha`);
        return response.data;
      } catch (error) {
        throw error.response?.data || error;
      }
    },

    // 获取注册验证码
    async getRegisterCaptcha() {
      try {
        const response = await axios.get(`${API_BASE_URL}/auth/register/captcha`);
        return response.data;
      } catch (error) {
        throw error.response?.data || error;
      }
    },

    // 用户登录
    async login(loginData) {
      try {
        const response = await axios.post(`${API_BASE_URL}/auth/login`, loginData);
        return response.data;
      } catch (error) {
        throw error.response?.data || error;
      }
    },

    // 用户注册
    async register(registerData) {
      try {
        const response = await axios.post(`${API_BASE_URL}/auth/register`, registerData);
        return response.data;
      } catch (error) {
        throw error.response?.data || error;
      }
    },

    // 退出登录
    async logout() {
      try {
        const response = await axios.get(`${API_BASE_URL}/auth/logout`);
        return response.data;
      } catch (error) {
        throw error.response?.data || error;
      }
    },

    // 获取用户信息
    async getUserInfo() {
      try {
        const response = await axios.get(`${API_BASE_URL}/auth/getUserInfo`);
        return response.data;
      } catch (error) {
        throw error.response?.data || error;
      }
    },

    // 修改密码
    async updatePassword(passwordData) {
      try {
        const response = await axios.post(`${API_BASE_URL}/auth/updatePwd`, passwordData);
        return response.data;
      } catch (error) {
        throw error.response?.data || error;
      }
    }
  }
};

export default api; 