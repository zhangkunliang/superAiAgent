package com.zkl.zklaiagent.controller;

import com.zkl.zklaiagent.constant.FileConstant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文件下载控制器
 * 提供PDF文件的下载、列表、删除等功能
 */
@RestController
@RequestMapping("/api/files")
@Tag(name = "文件管理", description = "文件下载、列表、删除等操作")
public class FileController {

    private static final String PDF_DIR = FileConstant.FILE_SAVE_DIR + "/pdf";
    private static final String DOWNLOAD_DIR = FileConstant.FILE_SAVE_DIR + "/download";
    private static final String FILE_DIR = FileConstant.FILE_SAVE_DIR + "/file";

    /**
     * 下载PDF文件
     */
    @GetMapping("/pdf/download/{fileName}")
    @Operation(summary = "下载PDF文件", description = "根据文件名下载指定的PDF文件")
    public ResponseEntity<Resource> downloadPdf(
            @Parameter(description = "PDF文件名", required = true)
            @PathVariable String fileName) {
        
        try {
            // 安全检查：防止路径遍历攻击
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                return ResponseEntity.badRequest().build();
            }
            
            // 确保文件名以.pdf结尾
            if (!fileName.toLowerCase().endsWith(".pdf")) {
                fileName += ".pdf";
            }
            
            Path filePath = Paths.get(PDF_DIR, fileName);
            File file = filePath.toFile();
            
            if (!file.exists() || !file.isFile()) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(file);
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", encodeFileName(fileName));
            headers.setContentLength(file.length());
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
                    
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 列出所有可下载的PDF文件
     */
    @GetMapping("/pdf/list")
    @Operation(summary = "获取PDF文件列表", description = "获取所有可下载的PDF文件列表及其信息")
    public ResponseEntity<List<FileInfo>> listPdfFiles() {
        try {
            File pdfDir = new File(PDF_DIR);
            if (!pdfDir.exists() || !pdfDir.isDirectory()) {
                return ResponseEntity.ok(Collections.emptyList());
            }
            
            File[] files = pdfDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
            if (files == null) {
                return ResponseEntity.ok(Collections.emptyList());
            }
            
            List<FileInfo> fileInfos = Arrays.stream(files)
                    .map(this::createFileInfo)
                    .sorted(Comparator.comparing(FileInfo::getLastModified).reversed())
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(fileInfos);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 删除PDF文件
     */
    @DeleteMapping("/pdf/{fileName}")
    @Operation(summary = "删除PDF文件", description = "删除指定的PDF文件")
    public ResponseEntity<Map<String, String>> deletePdf(
            @Parameter(description = "PDF文件名", required = true)
            @PathVariable String fileName) {
        
        Map<String, String> response = new HashMap<>();
        
        try {
            // 安全检查
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                response.put("error", "Invalid file name");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!fileName.toLowerCase().endsWith(".pdf")) {
                fileName += ".pdf";
            }
            
            Path filePath = Paths.get(PDF_DIR, fileName);
            File file = filePath.toFile();
            
            if (!file.exists()) {
                response.put("error", "File not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            if (file.delete()) {
                response.put("message", "File deleted successfully");
                response.put("fileName", fileName);
                return ResponseEntity.ok(response);
            } else {
                response.put("error", "Failed to delete file");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
        } catch (Exception e) {
            response.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取文件信息
     */
    @GetMapping("/pdf/info/{fileName}")
    @Operation(summary = "获取PDF文件信息", description = "获取指定PDF文件的详细信息")
    public ResponseEntity<FileInfo> getPdfInfo(
            @Parameter(description = "PDF文件名", required = true)
            @PathVariable String fileName) {
        
        try {
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                return ResponseEntity.badRequest().build();
            }
            
            if (!fileName.toLowerCase().endsWith(".pdf")) {
                fileName += ".pdf";
            }
            
            Path filePath = Paths.get(PDF_DIR, fileName);
            File file = filePath.toFile();
            
            if (!file.exists() || !file.isFile()) {
                return ResponseEntity.notFound().build();
            }
            
            FileInfo fileInfo = createFileInfo(file);
            return ResponseEntity.ok(fileInfo);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 下载其他类型文件
     */
    @GetMapping("/download/{fileName}")
    @Operation(summary = "下载文件", description = "下载指定的文件（支持多种格式）")
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "文件名", required = true)
            @PathVariable String fileName) {
        
        try {
            // 安全检查
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                return ResponseEntity.badRequest().build();
            }
            
            // 尝试从不同目录查找文件
            File file = findFileInDirectories(fileName, DOWNLOAD_DIR, FILE_DIR);
            
            if (file == null || !file.exists() || !file.isFile()) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(file);
            
            // 根据文件扩展名设置Content-Type
            MediaType mediaType = getMediaTypeForFile(fileName);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(mediaType);
            headers.setContentDispositionFormData("attachment", encodeFileName(fileName));
            headers.setContentLength(file.length());
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
                    
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 创建文件信息对象
     */
    private FileInfo createFileInfo(File file) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName(file.getName());
        fileInfo.setFileSize(file.length());
        fileInfo.setFileSizeFormatted(formatFileSize(file.length()));
        
        LocalDateTime lastModified = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(file.lastModified()),
                ZoneId.systemDefault()
        );
        fileInfo.setLastModified(lastModified);
        fileInfo.setLastModifiedFormatted(lastModified.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        // 生成下载URL
        String downloadUrl = "/api/files/pdf/download/" + file.getName();
        fileInfo.setDownloadUrl(downloadUrl);
        
        return fileInfo;
    }

    /**
     * 在多个目录中查找文件
     */
    private File findFileInDirectories(String fileName, String... directories) {
        for (String directory : directories) {
            File file = new File(directory, fileName);
            if (file.exists() && file.isFile()) {
                return file;
            }
        }
        return null;
    }

    /**
     * 根据文件扩展名获取MediaType
     */
    private MediaType getMediaTypeForFile(String fileName) {
        String extension = getFileExtension(fileName).toLowerCase();
        switch (extension) {
            case "pdf":
                return MediaType.APPLICATION_PDF;
            case "txt":
                return MediaType.TEXT_PLAIN;
            case "json":
                return MediaType.APPLICATION_JSON;
            case "xml":
                return MediaType.APPLICATION_XML;
            case "jpg":
            case "jpeg":
                return MediaType.IMAGE_JPEG;
            case "png":
                return MediaType.IMAGE_PNG;
            case "gif":
                return MediaType.IMAGE_GIF;
            default:
                return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1) : "";
    }

    /**
     * 编码文件名以支持中文
     */
    private String encodeFileName(String fileName) {
        try {
            return URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            return fileName;
        }
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * 获取系统统计信息
     */
    @GetMapping("/stats")
    @Operation(summary = "获取系统统计信息", description = "获取文件数量、总大小等统计信息")
    public ResponseEntity<SystemStats> getSystemStats() {
        try {
            File pdfDir = new File(PDF_DIR);
            SystemStats stats = new SystemStats();

            if (pdfDir.exists() && pdfDir.isDirectory()) {
                File[] files = pdfDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
                if (files != null) {
                    stats.setTotalFiles(files.length);

                    long totalSize = 0;
                    long lastModified = 0;

                    for (File file : files) {
                        totalSize += file.length();
                        lastModified = Math.max(lastModified, file.lastModified());
                    }

                    stats.setTotalSize(totalSize);
                    stats.setTotalSizeFormatted(formatFileSize(totalSize));

                    if (lastModified > 0) {
                        LocalDateTime lastUpdate = LocalDateTime.ofInstant(
                                java.time.Instant.ofEpochMilli(lastModified),
                                ZoneId.systemDefault()
                        );
                        stats.setLastUpdate(lastUpdate);
                        stats.setLastUpdateFormatted(lastUpdate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    }
                }
            }

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 系统统计信息DTO
     */
    public static class SystemStats {
        private int totalFiles = 0;
        private long totalSize = 0;
        private String totalSizeFormatted = "0 B";
        private LocalDateTime lastUpdate;
        private String lastUpdateFormatted;

        // Getters and Setters
        public int getTotalFiles() { return totalFiles; }
        public void setTotalFiles(int totalFiles) { this.totalFiles = totalFiles; }

        public long getTotalSize() { return totalSize; }
        public void setTotalSize(long totalSize) { this.totalSize = totalSize; }

        public String getTotalSizeFormatted() { return totalSizeFormatted; }
        public void setTotalSizeFormatted(String totalSizeFormatted) { this.totalSizeFormatted = totalSizeFormatted; }

        public LocalDateTime getLastUpdate() { return lastUpdate; }
        public void setLastUpdate(LocalDateTime lastUpdate) { this.lastUpdate = lastUpdate; }

        public String getLastUpdateFormatted() { return lastUpdateFormatted; }
        public void setLastUpdateFormatted(String lastUpdateFormatted) { this.lastUpdateFormatted = lastUpdateFormatted; }
    }

    /**
     * 文件信息DTO
     */
    public static class FileInfo {
        private String fileName;
        private long fileSize;
        private String fileSizeFormatted;
        private LocalDateTime lastModified;
        private String lastModifiedFormatted;
        private String downloadUrl;

        // Getters and Setters
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        
        public String getFileSizeFormatted() { return fileSizeFormatted; }
        public void setFileSizeFormatted(String fileSizeFormatted) { this.fileSizeFormatted = fileSizeFormatted; }
        
        public LocalDateTime getLastModified() { return lastModified; }
        public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }
        
        public String getLastModifiedFormatted() { return lastModifiedFormatted; }
        public void setLastModifiedFormatted(String lastModifiedFormatted) { this.lastModifiedFormatted = lastModifiedFormatted; }
        
        public String getDownloadUrl() { return downloadUrl; }
        public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    }
}
