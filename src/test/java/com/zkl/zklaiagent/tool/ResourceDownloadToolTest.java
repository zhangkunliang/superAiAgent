package com.zkl.zklaiagent.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ResourceDownloadTool测试类
 */
class ResourceDownloadToolTest {

    private ResourceDownloadTool resourceDownloadTool;

    @BeforeEach
    void setUp() {
        resourceDownloadTool = new ResourceDownloadTool();
    }

    @Test
    void testDownloadResourceWithValidUrl() {
        // 使用一个公开的测试URL
        String url = "https://httpbin.org/json";
        String fileName = "test.json";
        
        String result = resourceDownloadTool.downloadResource(url, fileName);
        
        assertNotNull(result);
        assertTrue(result.contains("Resource downloaded successfully"));
        assertFalse(result.contains("Error"));
        
        System.out.println("Download Result: " + result);
    }

    @Test
    void testDownloadResourceWithInvalidUrl() {
        String invalidUrl = "\\code\\superAiAgent";  // 模拟原始错误
        String fileName = "test.txt";
        
        String result = resourceDownloadTool.downloadResource(invalidUrl, fileName);
        
        assertNotNull(result);
        assertTrue(result.contains("Error: Invalid URL format"));
        assertTrue(result.contains("must start with http:// or https://"));
        
        System.out.println("Invalid URL Result: " + result);
    }

    @Test
    void testDownloadResourceWithNullUrl() {
        String result = resourceDownloadTool.downloadResource(null, "test.txt");
        
        assertNotNull(result);
        assertTrue(result.contains("Error: URL cannot be null or empty"));
        
        System.out.println("Null URL Result: " + result);
    }

    @Test
    void testDownloadResourceWithEmptyUrl() {
        String result = resourceDownloadTool.downloadResource("", "test.txt");
        
        assertNotNull(result);
        assertTrue(result.contains("Error: URL cannot be null or empty"));
        
        System.out.println("Empty URL Result: " + result);
    }

    @Test
    void testDownloadResourceWithNullFileName() {
        String result = resourceDownloadTool.downloadResource("https://example.com/test.txt", null);
        
        assertNotNull(result);
        assertTrue(result.contains("Error: File name cannot be null or empty"));
        
        System.out.println("Null FileName Result: " + result);
    }

    @Test
    void testDownloadResourceWithEmptyFileName() {
        String result = resourceDownloadTool.downloadResource("https://example.com/test.txt", "");
        
        assertNotNull(result);
        assertTrue(result.contains("Error: File name cannot be null or empty"));
        
        System.out.println("Empty FileName Result: " + result);
    }

    @Test
    void testDownloadResourceWithInvalidProtocol() {
        String invalidUrl = "ftp://example.com/file.txt";
        String fileName = "test.txt";
        
        String result = resourceDownloadTool.downloadResource(invalidUrl, fileName);
        
        assertNotNull(result);
        assertTrue(result.contains("Error: Invalid URL format"));
        
        System.out.println("Invalid Protocol Result: " + result);
    }

    @Test
    void testCopyLocalFileWithValidFile(@TempDir Path tempDir) throws IOException {
        // 创建一个临时文件
        Path sourceFile = tempDir.resolve("source.txt");
        Files.write(sourceFile, "Test content".getBytes());
        
        String result = resourceDownloadTool.copyLocalFile(sourceFile.toString(), "copied.txt");
        
        assertNotNull(result);
        assertTrue(result.contains("File copied successfully"));
        assertFalse(result.contains("Error"));
        
        System.out.println("Copy File Result: " + result);
    }

    @Test
    void testCopyLocalFileWithNonExistentFile() {
        String nonExistentPath = "/path/that/does/not/exist.txt";
        
        String result = resourceDownloadTool.copyLocalFile(nonExistentPath, "test.txt");
        
        assertNotNull(result);
        assertTrue(result.contains("Error: Source file does not exist"));
        
        System.out.println("Non-existent File Result: " + result);
    }

    @Test
    void testCopyLocalFileWithDirectory(@TempDir Path tempDir) {
        String result = resourceDownloadTool.copyLocalFile(tempDir.toString(), "test.txt");
        
        assertNotNull(result);
        assertTrue(result.contains("Error: Source path is not a file"));
        
        System.out.println("Directory Path Result: " + result);
    }

    @Test
    void testCopyLocalFileWithNullPath() {
        String result = resourceDownloadTool.copyLocalFile(null, "test.txt");
        
        assertNotNull(result);
        assertTrue(result.contains("Error: Source path cannot be null or empty"));
        
        System.out.println("Null Path Result: " + result);
    }
}
