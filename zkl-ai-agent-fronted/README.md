# 天王星 AI 智能体平台

这是一个基于 Vue3 的前端项目，为天王星数字科技有限公司的 AI 智能体平台提供界面。

## 功能特点

- 响应式设计，支持 PC 端、平板和手机
- 多应用入口，包括疾控监督专家和超级智能体
- 实时聊天功能，使用 SSE 流式获取 AI 回复
- Markdown 格式支持，美观展示 AI 回复内容
- 会话历史保存，随时可以查看历史对话

## 技术栈

- Vue 3 - 前端框架
- Vue Router - 路由管理
- Pinia - 状态管理
- Axios - HTTP 请求
- Marked - Markdown 解析

## 项目结构

```
src/
├── assets/           # 静态资源
├── components/       # 可复用组件
├── router/           # 路由配置
├── store/            # 状态管理
├── utils/            # 工具函数
├── views/            # 页面视图
├── App.vue           # 根组件
└── main.js           # 入口文件
```

## 开发指南

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

### 构建生产版本

```bash
npm run build
```

## 接口说明

后端接口基于 Spring Boot，提供了以下 API 端点：

- `/api/ai/inspector_app/chat/sse` - 疾控监督专家聊天接口
- `/api/ai/manus/chat` - 超级智能体聊天接口

## 许可证

版权所有 © 天王星数字科技有限公司 