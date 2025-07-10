package com.zkl.zklaiagent.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FileController编译测试
 * 验证修复后的代码能够正常编译
 */
class FileControllerCompileTest {

    @Test
    void testResponseEntityUsage() {
        // 测试正确的ResponseEntity用法
        
        // 1. badRequest() 可以携带body
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Invalid file name");
        ResponseEntity<Map<String, String>> badRequestResponse = 
            ResponseEntity.badRequest().body(errorResponse);
        assertEquals(HttpStatus.BAD_REQUEST, badRequestResponse.getStatusCode());
        assertNotNull(badRequestResponse.getBody());
        
        // 2. notFound() 不能携带body，应该使用status()
        ResponseEntity<Map<String, String>> notFoundResponse = 
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        assertEquals(HttpStatus.NOT_FOUND, notFoundResponse.getStatusCode());
        assertNotNull(notFoundResponse.getBody());
        
        // 3. ok() 可以携带body
        Map<String, String> successResponse = new HashMap<>();
        successResponse.put("message", "Success");
        ResponseEntity<Map<String, String>> okResponse = 
            ResponseEntity.ok(successResponse);
        assertEquals(HttpStatus.OK, okResponse.getStatusCode());
        assertNotNull(okResponse.getBody());
        
        // 4. status() 可以携带body
        ResponseEntity<Map<String, String>> statusResponse = 
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, statusResponse.getStatusCode());
        assertNotNull(statusResponse.getBody());
    }

    @Test
    void testFileControllerInstantiation() {
        // 测试FileController能够正常实例化
        assertDoesNotThrow(() -> {
            FileController controller = new FileController();
            assertNotNull(controller);
        });
    }

    @Test
    void testFileInfoClass() {
        // 测试FileInfo内部类
        assertDoesNotThrow(() -> {
            FileController.FileInfo fileInfo = new FileController.FileInfo();
            fileInfo.setFileName("test.pdf");
            fileInfo.setFileSize(1024);
            fileInfo.setFileSizeFormatted("1.0 KB");
            
            assertEquals("test.pdf", fileInfo.getFileName());
            assertEquals(1024, fileInfo.getFileSize());
            assertEquals("1.0 KB", fileInfo.getFileSizeFormatted());
        });
    }

    @Test
    void testHttpStatusConstants() {
        // 验证使用的HTTP状态码常量存在
        assertNotNull(HttpStatus.OK);
        assertNotNull(HttpStatus.BAD_REQUEST);
        assertNotNull(HttpStatus.NOT_FOUND);
        assertNotNull(HttpStatus.INTERNAL_SERVER_ERROR);
        
        assertEquals(200, HttpStatus.OK.value());
        assertEquals(400, HttpStatus.BAD_REQUEST.value());
        assertEquals(404, HttpStatus.NOT_FOUND.value());
        assertEquals(500, HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
