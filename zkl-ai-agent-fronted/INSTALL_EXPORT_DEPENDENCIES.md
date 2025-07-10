# 导出功能依赖安装指南

## 问题说明

由于系统权限或网络问题，可能无法直接通过 npm 安装导出功能所需的依赖包。

## 解决方案

### 方案1：修改 npm 配置（推荐）

1. **以管理员身份运行命令提示符**
2. **设置 npm 缓存目录到用户目录**：
   ```bash
   npm config set cache "C:\Users\%USERNAME%\AppData\Roaming\npm-cache" --global
   ```
3. **安装依赖**：
   ```bash
   npm install docx jspdf html2canvas
   ```

### 方案2：使用 yarn（如果已安装）

```bash
yarn add docx jspdf html2canvas
```

### 方案3：手动下载依赖

如果上述方法都不行，可以手动下载依赖包：

1. 访问 [npmjs.com](https://www.npmjs.com/)
2. 搜索并下载以下包：
   - `docx`
   - `jspdf` 
   - `html2canvas`
3. 解压到 `node_modules` 目录

### 方案4：使用 CDN（临时解决方案）

在 `public/index.html` 中添加 CDN 链接：

```html
<script src="https://unpkg.com/docx@8.5.0/build/index.js"></script>
<script src="https://unpkg.com/jspdf@2.5.1/dist/jspdf.umd.min.js"></script>
<script src="https://unpkg.com/html2canvas@1.4.1/dist/html2canvas.min.js"></script>
```

## 当前状态

即使没有安装这些依赖包，应用仍然可以正常运行：

- ✅ **基本功能**：聊天、换行符修复等核心功能正常
- ✅ **导出按钮**：会显示导出按钮
- ⚠️ **导出功能**：会自动降级为纯文本导出（.txt 格式）

## 功能降级说明

当依赖包不可用时，导出功能会自动降级：

1. **Word 导出** → **文本导出**
2. **PDF 导出** → **文本导出**
3. **保留所有内容**：包括时间戳、发送者信息、格式化文本

## 验证安装

安装完成后，可以通过以下方式验证：

1. 重启开发服务器：`npm run dev`
2. 在聊天界面发送消息
3. 尝试点击导出按钮
4. 检查浏览器控制台是否有错误信息

## 故障排除

### 常见错误

1. **权限错误 (EPERM)**
   - 以管理员身份运行命令提示符
   - 关闭杀毒软件的实时保护
   - 确保 Node.js 安装目录有写权限

2. **网络错误**
   - 检查网络连接
   - 尝试使用国内镜像：
     ```bash
     npm config set registry https://registry.npmmirror.com
     ```

3. **缓存问题**
   - 清理 npm 缓存：
     ```bash
     npm cache clean --force
     ```

## 生产环境部署

在生产环境中，建议：

1. 使用 Docker 容器化部署
2. 在构建阶段安装所有依赖
3. 使用 CI/CD 流水线自动化部署

## 联系支持

如果仍然无法解决依赖安装问题，请：

1. 检查 Node.js 版本（建议 16+）
2. 提供完整的错误日志
3. 说明操作系统版本和权限情况
