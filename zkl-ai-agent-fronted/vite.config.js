import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8123',
        changeOrigin: true,
      }
    }
  },
  build: {
    // 设置chunk大小警告限制为2000KB
    chunkSizeWarningLimit: 2000,
    // 构建输出目录
    outDir: 'dist',
    // 公共基础路径
    base: './',
    rollupOptions: {
      output: {
        // 手动分割代码块
        manualChunks: {
          // Vue相关
          'vue-vendor': ['vue', 'vue-router'],
          // Pinia状态管理
          'pinia-vendor': ['pinia'],
          // 工具库
          'utils-vendor': ['axios'],
          // 文档处理库
          'document-vendor': ['docx', 'jspdf', 'html2canvas'],
          // 其他工具库
          'highlight-vendor': ['highlight.js', 'marked']
        },
        // 文件命名策略 - 与nginx配置匹配
        chunkFileNames: 'js/[name]-[hash].js',
        entryFileNames: 'js/[name]-[hash].js',
        assetFileNames: (assetInfo) => {
          const info = assetInfo.name.split('.')
          let extType = info[info.length - 1]
          if (/\.(png|jpe?g|gif|svg)(\?.*)?$/i.test(assetInfo.name)) {
            return 'img/[name]-[hash].[ext]'
          } else if (/\.(woff2?|eot|ttf|otf)(\?.*)?$/i.test(assetInfo.name)) {
            return 'fonts/[name]-[hash].[ext]'
          } else if (/\.(css)(\?.*)?$/i.test(assetInfo.name)) {
            return 'css/[name]-[hash].[ext]'
          }
          return 'assets/[name]-[hash].[ext]'
        }
      }
    },
    // 启用代码压缩
    minify: 'esbuild',
    // 移除console和debugger
    esbuild: {
      drop: ['console', 'debugger'],
    },
  }
}) 