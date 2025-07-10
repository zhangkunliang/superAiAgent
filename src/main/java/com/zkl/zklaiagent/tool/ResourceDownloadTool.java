package com.zkl.zklaiagent.tool;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.zkl.zklaiagent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * 资源下载工具
 */
public class ResourceDownloadTool {

    @Tool(description = "Download a resource from a given URL. The URL must be a valid HTTP/HTTPS URL starting with http:// or https://")
    public String downloadResource(
            @ToolParam(description = "Valid HTTP/HTTPS URL of the resource to download (e.g., https://example.com/file.pdf)") String url,
            @ToolParam(description = "Name of the file to save the downloaded resource") String fileName) {

        // 验证输入参数
        if (url == null || url.trim().isEmpty()) {
            return "Error: URL cannot be null or empty";
        }

        if (fileName == null || fileName.trim().isEmpty()) {
            return "Error: File name cannot be null or empty";
        }

        // 验证URL格式
        if (!isValidUrl(url)) {
            return "Error: Invalid URL format. URL must start with http:// or https://. Received: " + url;
        }

        String fileDir = FileConstant.FILE_SAVE_DIR + "/download";
        String filePath = fileDir + "/" + fileName;

        try {
            // 创建目录
            FileUtil.mkdir(fileDir);

            // 使用 Hutool 的 downloadFile 方法下载资源
            HttpUtil.downloadFile(url, new File(filePath));
            return "Resource downloaded successfully to: " + filePath;

        } catch (Exception e) {
            return "Error downloading resource: " + e.getClass().getSimpleName() + " - " + e.getMessage();
        }
    }

    /**
     * 验证URL格式是否有效
     */
    private boolean isValidUrl(String url) {
        try {
            // 检查URL是否以http://或https://开头
            if (!url.toLowerCase().startsWith("http://") && !url.toLowerCase().startsWith("https://")) {
                return false;
            }

            // 尝试创建URL对象来验证格式
            new URL(url);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    @Tool(description = "Copy a local file to the download directory")
    public String copyLocalFile(
            @ToolParam(description = "Full path to the local file to copy") String sourcePath,
            @ToolParam(description = "Name of the destination file") String fileName) {

        // 验证输入参数
        if (sourcePath == null || sourcePath.trim().isEmpty()) {
            return "Error: Source path cannot be null or empty";
        }

        if (fileName == null || fileName.trim().isEmpty()) {
            return "Error: File name cannot be null or empty";
        }

        String fileDir = FileConstant.FILE_SAVE_DIR + "/download";
        String destPath = fileDir + "/" + fileName;

        try {
            // 创建目录
            FileUtil.mkdir(fileDir);

            // 检查源文件是否存在
            File sourceFile = new File(sourcePath);
            if (!sourceFile.exists()) {
                return "Error: Source file does not exist: " + sourcePath;
            }

            if (!sourceFile.isFile()) {
                return "Error: Source path is not a file: " + sourcePath;
            }

            // 复制文件
            FileUtil.copy(sourceFile, new File(destPath), true);
            return "File copied successfully to: " + destPath;

        } catch (Exception e) {
            return "Error copying file: " + e.getClass().getSimpleName() + " - " + e.getMessage();
        }
    }
}
