package com.zkl.zklaiagent.tool;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.dashscope.utils.JsonUtils;
import com.zkl.zklaiagent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * DashScope多模态对话工具类
 * 支持文本和图像的多模态对话，基于阿里云DashScope API
 */
public class DashScopeMultiModalTool {

    // DashScope API Key
    private final String apiKey;

    // 默认模型
    private static final String DEFAULT_MODEL = "qwen-vl-plus";

    // 临时图片存储目录
    private static final String TEMP_IMAGE_DIR = FileConstant.FILE_SAVE_DIR + "/temp_images";

    // 服务器基础URL（用于生成图片访问URL）
    private String serverBaseUrl = "http://localhost:8123/api";

    public DashScopeMultiModalTool(String apiKey) {
        this.apiKey = apiKey;
        // 确保临时图片目录存在
        FileUtil.mkdir(TEMP_IMAGE_DIR);
    }

    public DashScopeMultiModalTool(String apiKey, String serverBaseUrl) {
        this.apiKey = apiKey;
        this.serverBaseUrl = serverBaseUrl;
        FileUtil.mkdir(TEMP_IMAGE_DIR);
    }

    @Tool(description = "Analyze images and answer questions using DashScope multimodal AI. Supports multiple images and text input for comprehensive visual understanding and conversation")
    public String analyzeImagesWithText(
            @ToolParam(description = "Text question or description about the images") String text,
            @ToolParam(description = "Base64 encoded image data (single image)") String imageBase64,
            @ToolParam(description = "Model name to use (default: qwen-vl-plus)") String model) {

        if (StrUtil.isBlank(text) && StrUtil.isBlank(imageBase64)) {
            return "Error: Both text and image cannot be empty";
        }

        if (StrUtil.isBlank(model)) {
            model = DEFAULT_MODEL;
        }

        try {
            List<Map<String, Object>> contentList = new ArrayList<>();

            // 添加图片内容
            if (StrUtil.isNotBlank(imageBase64)) {
                String imageUrl = convertBase64ToUrl(imageBase64);
                contentList.add(Collections.singletonMap("image", imageUrl));
            }

            // 添加文本内容
            if (StrUtil.isNotBlank(text)) {
                contentList.add(Collections.singletonMap("text", text));
            }

            // 构建多模态消息
            MultiModalMessage userMessage = MultiModalMessage.builder()
                    .role(Role.USER.getValue())
                    .content(contentList)
                    .build();

            // 构建请求参数
            MultiModalConversationParam param = MultiModalConversationParam.builder()
                    .apiKey(apiKey)
                    .model(model)
                    .message(userMessage)
                    .build();

            // 调用API
            MultiModalConversation conv = new MultiModalConversation();
            MultiModalConversationResult result = conv.call(param);

            // 解析响应
            if (result != null && result.getOutput() != null && result.getOutput().getChoices() != null) {
                return result.getOutput().getChoices().get(0).getMessage().getContent().get(0).get("text").toString();
            }

            return "No response received from DashScope";

        } catch (ApiException | NoApiKeyException | UploadFileException e) {
            return "Error calling DashScope API: " + e.getMessage();
        } catch (Exception e) {
            return "Error processing multimodal request: " + e.getMessage();
        }
    }

    @Tool(description = "Analyze multiple images with text using DashScope multimodal AI")
    public String analyzeMultipleImages(
            @ToolParam(description = "Text question about the images") String text,
            @ToolParam(description = "Comma-separated base64 encoded images") String imagesBase64,
            @ToolParam(description = "Model name (default: qwen-vl-plus)") String model) {

        if (StrUtil.isBlank(text)) {
            return "Error: Text question is required";
        }

        if (StrUtil.isBlank(imagesBase64)) {
            return "Error: At least one image is required";
        }

        if (StrUtil.isBlank(model)) {
            model = DEFAULT_MODEL;
        }

        try {
            List<Map<String, Object>> contentList = new ArrayList<>();

            // 处理多个图片
            String[] imageArray = imagesBase64.split(",");
            for (String imageBase64 : imageArray) {
                if (StrUtil.isNotBlank(imageBase64.trim())) {
                    String imageUrl = convertBase64ToUrl(imageBase64.trim());
                    contentList.add(Collections.singletonMap("image", imageUrl));
                }
            }

            // 添加文本
            contentList.add(Collections.singletonMap("text", text));

            // 构建消息和参数
            MultiModalMessage userMessage = MultiModalMessage.builder()
                    .role(Role.USER.getValue())
                    .content(contentList)
                    .build();

            MultiModalConversationParam param = MultiModalConversationParam.builder()
                    .apiKey(apiKey)
                    .model(model)
                    .message(userMessage)
                    .build();

            // 调用API
            MultiModalConversation conv = new MultiModalConversation();
            MultiModalConversationResult result = conv.call(param);

            // 解析响应
            if (result != null && result.getOutput() != null && result.getOutput().getChoices() != null) {
                return result.getOutput().getChoices().get(0).getMessage().getContent().get(0).get("text").toString();
            }

            return "No response received from DashScope";

        } catch (Exception e) {
            return "Error processing multiple images: " + e.getMessage();
        }
    }

    @Tool(description = "Simple image analysis with DashScope AI")
    public String askAboutImage(
            @ToolParam(description = "Question about the image") String question,
            @ToolParam(description = "Base64 encoded image") String imageBase64) {
        return analyzeImagesWithText(question, imageBase64, DEFAULT_MODEL);
    }

    /**
     * 将Base64图片转换为可访问的URL
     * 解决base64格式图片无法直接用于API调用的问题
     */
    private String convertBase64ToUrl(String base64Data) throws IOException {
        // 移除base64前缀（如果存在）
        String cleanBase64 = base64Data;
        if (base64Data.contains(",")) {
            cleanBase64 = base64Data.split(",")[1];
        }

        // 解码base64数据
        byte[] imageBytes = Base64.getDecoder().decode(cleanBase64);

        // 生成唯一文件名
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String randomId = IdUtil.simpleUUID().substring(0, 8);
        String fileName = String.format("temp_image_%s_%s.jpg", timestamp, randomId);

        // 保存到临时目录
        String filePath = TEMP_IMAGE_DIR + "/" + fileName;
        Files.write(Paths.get(filePath), imageBytes);

        // 返回可访问的URL
        return serverBaseUrl + "/files/temp-images/" + fileName;
    }

    /**
     * 清理过期的临时图片文件
     */
    @Tool(description = "Clean up expired temporary image files")
    public String cleanupTempImages() {
        try {
            File tempDir = new File(TEMP_IMAGE_DIR);
            if (!tempDir.exists()) {
                return "Temp image directory does not exist";
            }

            File[] files = tempDir.listFiles();
            if (files == null || files.length == 0) {
                return "No temporary images to clean";
            }

            int deletedCount = 0;
            long currentTime = System.currentTimeMillis();
            long oneHourAgo = currentTime - (60 * 60 * 1000); // 1小时前

            for (File file : files) {
                if (file.isFile() && file.lastModified() < oneHourAgo) {
                    if (file.delete()) {
                        deletedCount++;
                    }
                }
            }

            return String.format("Cleaned up %d expired temporary images", deletedCount);

        } catch (Exception e) {
            return "Error cleaning temp images: " + e.getMessage();
        }
    }

    /**
     * 列出可用的模型
     */
    public String[] getAvailableModels() {
        return new String[]{
            "qwen-vl-plus",      // 通义千问VL增强版
            "qwen-vl-max",       // 通义千问VL旗舰版
            "qwen-vl-ocr"        // 通义千问VL OCR版
        };
    }

    /**
     * 获取临时图片统计信息
     */
    @Tool(description = "Get statistics about temporary images")
    public String getTempImageStats() {
        try {
            File tempDir = new File(TEMP_IMAGE_DIR);
            if (!tempDir.exists()) {
                return "Temp image directory does not exist";
            }

            File[] files = tempDir.listFiles();
            if (files == null) {
                return "Cannot access temp image directory";
            }

            int totalFiles = files.length;
            long totalSize = 0;
            int recentFiles = 0;
            long currentTime = System.currentTimeMillis();
            long oneHourAgo = currentTime - (60 * 60 * 1000);

            for (File file : files) {
                if (file.isFile()) {
                    totalSize += file.length();
                    if (file.lastModified() > oneHourAgo) {
                        recentFiles++;
                    }
                }
            }

            double totalSizeMB = totalSize / (1024.0 * 1024.0);

            return String.format(
                "Temporary Images Statistics:\n" +
                "- Total files: %d\n" +
                "- Total size: %.2f MB\n" +
                "- Recent files (last hour): %d\n" +
                "- Directory: %s",
                totalFiles, totalSizeMB, recentFiles, TEMP_IMAGE_DIR
            );

        } catch (Exception e) {
            return "Error getting temp image stats: " + e.getMessage();
        }
    }
}
