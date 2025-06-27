import { defineStore } from 'pinia'

export const useChatStore = defineStore('chat', {
  state: () => ({
    inspectorChats: {},
    manusChats: {},
  }),

  actions: {
    // 添加一条用户消息到Inspector聊天记录
    addInspectorUserMessage(chatId, message) {
      if (!this.inspectorChats[chatId]) {
        this.inspectorChats[chatId] = [];
      }
      this.inspectorChats[chatId].push({
        role: 'user',
        content: message,
        timestamp: new Date().toISOString()
      });
    },

    // 添加一条AI消息到Inspector聊天记录
    addInspectorAiMessage(chatId, message) {
      if (!this.inspectorChats[chatId]) {
        this.inspectorChats[chatId] = [];
      }
      this.inspectorChats[chatId].push({
        role: 'assistant',
        content: message,
        timestamp: new Date().toISOString()
      });
    },

    // 添加一条用户消息到Manus聊天记录
    addManusUserMessage(chatId, message) {
      if (!this.manusChats[chatId]) {
        this.manusChats[chatId] = [];
      }
      this.manusChats[chatId].push({
        role: 'user',
        content: message,
        timestamp: new Date().toISOString()
      });
    },

    // 添加一条AI消息到Manus聊天记录
    addManusAiMessage(chatId, message) {
      if (!this.manusChats[chatId]) {
        this.manusChats[chatId] = [];
      }
      this.manusChats[chatId].push({
        role: 'assistant',
        content: message,
        timestamp: new Date().toISOString()
      });
    },

    // 获取Inspector聊天记录
    getInspectorChat(chatId) {
      return this.inspectorChats[chatId] || [];
    },

    // 获取Manus聊天记录
    getManusChat(chatId) {
      return this.manusChats[chatId] || [];
    },
    
    // 删除Inspector聊天记录
    deleteInspectorChat(chatId) {
      if (this.inspectorChats[chatId]) {
        const newChats = { ...this.inspectorChats };
        delete newChats[chatId];
        this.inspectorChats = newChats;
      }
    },
    
    // 删除Manus聊天记录
    deleteManusChat(chatId) {
      if (this.manusChats[chatId]) {
        const newChats = { ...this.manusChats };
        delete newChats[chatId];
        this.manusChats = newChats;
      }
    }
  },

  persist: {
    key: 'zkl-ai-agent-chat-history'
  }
}) 