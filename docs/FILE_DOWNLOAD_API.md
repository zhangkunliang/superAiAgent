# PDF文件下载API文档

## 概述

本文档描述了PDF文件下载管理系统的REST API接口。该系统提供了完整的PDF文件管理功能，包括文件列表、下载、删除和信息查询。

## 基础信息

- **基础URL**: `/api/files`
- **支持格式**: JSON
- **字符编码**: UTF-8

## API接口列表

### 1. 获取PDF文件列表

获取所有可下载的PDF文件列表及其详细信息。

**请求**
```
GET /api/files/pdf/list
```

**响应示例**
```json
[
  {
    "fileName": "疾控监督调研报告.pdf",
    "fileSize": 1048576,
    "fileSizeFormatted": "1.0 MB",
    "lastModified": "2024-01-15T10:30:00",
    "lastModifiedFormatted": "2024-01-15 10:30:00",
    "downloadUrl": "/api/files/pdf/download/疾控监督调研报告.pdf"
  },
  {
    "fileName": "疫情调研报告.pdf",
    "fileSize": 2097152,
    "fileSizeFormatted": "2.0 MB",
    "lastModified": "2024-01-14T15:45:00",
    "lastModifiedFormatted": "2024-01-14 15:45:00",
    "downloadUrl": "/api/files/pdf/download/疫情调研报告.pdf"
  }
]
```

### 2. 下载PDF文件

根据文件名下载指定的PDF文件。

**请求**
```
GET /api/files/pdf/download/{fileName}
```

**路径参数**
- `fileName` (string, required): PDF文件名

**响应**
- **成功**: 返回PDF文件流，Content-Type为`application/pdf`
- **404**: 文件不存在
- **400**: 文件名格式错误（包含非法字符）

**示例**
```bash
# 下载文件
curl -O "http://localhost:8123/api/files/pdf/download/疾控监督调研报告.pdf"

# 使用浏览器直接访问
http://localhost:8123/api/files/pdf/download/疾控监督调研报告.pdf
```

### 3. 获取PDF文件信息

获取指定PDF文件的详细信息。

**请求**
```
GET /api/files/pdf/info/{fileName}
```

**路径参数**
- `fileName` (string, required): PDF文件名

**响应示例**
```json
{
  "fileName": "疾控监督调研报告.pdf",
  "fileSize": 1048576,
  "fileSizeFormatted": "1.0 MB",
  "lastModified": "2024-01-15T10:30:00",
  "lastModifiedFormatted": "2024-01-15 10:30:00",
  "downloadUrl": "/api/files/pdf/download/疾控监督调研报告.pdf"
}
```

### 4. 删除PDF文件

删除指定的PDF文件。

**请求**
```
DELETE /api/files/pdf/{fileName}
```

**路径参数**
- `fileName` (string, required): PDF文件名

**响应示例**

**成功删除**
```json
{
  "message": "File deleted successfully",
  "fileName": "疾控监督调研报告.pdf"
}
```

**错误响应**
```json
{
  "error": "File not found"
}
```

### 5. 下载其他类型文件

下载非PDF格式的文件（支持多种文件类型）。

**请求**
```
GET /api/files/download/{fileName}
```

**路径参数**
- `fileName` (string, required): 文件名

**支持的文件类型**
- 文本文件: `.txt`, `.md`, `.json`, `.xml`
- 图片文件: `.jpg`, `.jpeg`, `.png`, `.gif`
- 其他文件: 以`application/octet-stream`类型下载

## 前端界面

系统提供了一个用户友好的Web界面来管理PDF文件。

**访问地址**
```
http://localhost:8123/api/pdf-download.html
```

**功能特性**
- 📋 文件列表展示
- 📊 统计信息（总文件数、总大小、最后更新时间）
- 🔍 文件搜索功能
- ⬇️ 一键下载
- 🗑️ 文件删除
- ℹ️ 文件详情查看
- 📱 响应式设计，支持移动设备

## 安全特性

### 1. 路径遍历防护
系统会检查并拒绝包含以下字符的文件名：
- `..` (父目录引用)
- `/` (Unix路径分隔符)
- `\` (Windows路径分隔符)

### 2. 文件类型验证
- PDF下载接口只允许下载`.pdf`文件
- 自动为不带扩展名的文件添加`.pdf`后缀

### 3. 文件存在性检查
- 验证文件是否存在且为有效文件
- 防止下载目录或其他非文件对象

## 错误处理

### HTTP状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 400 | 请求参数错误（如文件名包含非法字符） |
| 404 | 文件不存在 |
| 500 | 服务器内部错误 |

### 错误响应格式

```json
{
  "error": "错误描述信息"
}
```

## 使用示例

### JavaScript示例

```javascript
// 获取文件列表
async function getFileList() {
    const response = await fetch('/api/files/pdf/list');
    const files = await response.json();
    console.log('PDF files:', files);
}

// 下载文件
function downloadFile(fileName) {
    const url = `/api/files/pdf/download/${encodeURIComponent(fileName)}`;
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    a.click();
}

// 删除文件
async function deleteFile(fileName) {
    const response = await fetch(`/api/files/pdf/${encodeURIComponent(fileName)}`, {
        method: 'DELETE'
    });
    const result = await response.json();
    if (response.ok) {
        console.log('File deleted:', result.message);
    } else {
        console.error('Delete failed:', result.error);
    }
}
```

### cURL示例

```bash
# 获取文件列表
curl -X GET "http://localhost:8123/api/files/pdf/list"

# 下载文件
curl -O "http://localhost:8123/api/files/pdf/download/report.pdf"

# 获取文件信息
curl -X GET "http://localhost:8123/api/files/pdf/info/report.pdf"

# 删除文件
curl -X DELETE "http://localhost:8123/api/files/pdf/report.pdf"
```

## 配置说明

文件存储路径由`FileConstant.FILE_SAVE_DIR`常量定义：
- 默认路径: `{项目根目录}/tmp`
- PDF文件目录: `{项目根目录}/tmp/pdf`
- 下载文件目录: `{项目根目录}/tmp/download`
- 其他文件目录: `{项目根目录}/tmp/file`

## 注意事项

1. **文件名编码**: 支持中文文件名，系统会自动处理URL编码
2. **文件大小**: 建议单个文件不超过100MB
3. **并发下载**: 支持多用户同时下载
4. **缓存策略**: 浏览器会缓存下载的文件
5. **权限控制**: 当前版本未实现用户权限控制，所有用户都可以访问所有文件

## 扩展功能

未来可以考虑添加的功能：
- 用户认证和权限控制
- 文件上传功能
- 文件预览功能
- 批量操作（批量下载、批量删除）
- 文件分类和标签
- 搜索和过滤增强
- 文件版本管理
