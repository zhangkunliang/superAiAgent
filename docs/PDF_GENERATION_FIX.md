# PDF生成工具修复说明

## 问题描述

在使用PDFGenerationTool生成PDF文件时，出现以下错误：
```
Type of font STSongStd-Light is not recognized.
```

## 问题原因

1. **依赖配置问题**：`font-asian` 依赖的scope设置为 `test`，导致运行时无法使用亚洲字体
2. **字体不存在**：系统中没有 `STSongStd-Light` 字体或iText无法识别该字体
3. **缺少字体回退机制**：没有提供备用字体方案

## 解决方案

### 1. 修复依赖配置

将 `pom.xml` 中的 `font-asian` 依赖scope从 `test` 改为默认（运行时可用）：

```xml
<!-- 修复前 -->
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>font-asian</artifactId>
    <version>9.1.0</version>
    <scope>test</scope>  <!-- 问题：只在测试时可用 -->
</dependency>

<!-- 修复后 -->
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>font-asian</artifactId>
    <version>9.1.0</version>
    <!-- 移除scope，使其在运行时可用 -->
</dependency>
```

### 2. 增强字体选择机制

在 `PDFGenerationTool.java` 中添加了 `createChineseFont()` 方法，提供多层字体回退：

#### 方案1：系统字体
- Windows: `simsun.ttc`, `simhei.ttf`, `msyh.ttc`
- macOS: `PingFang.ttc`
- Linux: `DejaVuSans.ttf`

#### 方案2：iText内置字体
- `STSongStd-Light`
- `STSong-Light`
- `MSungStd-Light`
- `HeiseiKakuGo-W5`

#### 方案3：默认字体
- 如果以上都失败，使用系统默认字体

### 3. 改进的代码结构

```java
private PdfFont createChineseFont() {
    // 尝试系统字体
    for (String fontPath : systemFonts) {
        try {
            if (Files.exists(Paths.get(fontPath.split(",")[0]))) {
                return PdfFontFactory.createFont(fontPath, "Identity-H");
            }
        } catch (Exception e) {
            // 继续尝试下一个字体
        }
    }
    
    // 尝试内置字体
    for (String fontName : builtinFonts) {
        try {
            return PdfFontFactory.createFont(fontName, "UniGB-UCS2-H");
        } catch (Exception e) {
            // 继续尝试下一个字体
        }
    }
    
    // 使用默认字体
    return PdfFontFactory.createFont();
}
```

## 测试验证

创建了 `PDFGenerationToolTest.java` 测试类，包含以下测试用例：

1. **中文内容测试** - 验证中文字符显示
2. **英文内容测试** - 验证英文字符显示
3. **混合内容测试** - 验证中英文混合显示
4. **空内容测试** - 验证边界情况
5. **长内容测试** - 验证大量内容处理

## 使用建议

1. **重新编译项目**：修改依赖后需要重新编译
2. **清理缓存**：`mvn clean compile`
3. **运行测试**：`mvn test -Dtest=PDFGenerationToolTest`

## 预期效果

修复后，PDF生成工具应该能够：
- ✅ 正确显示中文字符
- ✅ 处理各种字符编码
- ✅ 在不同操作系统上工作
- ✅ 提供优雅的字体回退机制
- ✅ 避免字体识别错误

## 注意事项

1. 如果系统中没有中文字体，可能仍然无法正确显示中文字符
2. 建议在生产环境中安装常用的中文字体
3. 可以根据需要添加更多字体路径到系统字体列表中
