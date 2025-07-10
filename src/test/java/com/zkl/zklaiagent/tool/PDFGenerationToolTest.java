package com.zkl.zklaiagent.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PDFGenerationTool测试类
 */
class PDFGenerationToolTest {

    private PDFGenerationTool pdfGenerationTool;

    @BeforeEach
    void setUp() {
        pdfGenerationTool = new PDFGenerationTool();
    }

    @Test
    void testGeneratePDFWithChineseContent() {
        String fileName = "test_chinese.pdf";
        String content = "这是一个测试PDF文档。\n包含中文内容。\n测试字体支持。";
        
        String result = pdfGenerationTool.generatePDF(fileName, content);
        
        assertNotNull(result);
        assertTrue(result.contains("PDF generated successfully"));
        assertFalse(result.contains("Error"));
        
        System.out.println("PDF Generation Result: " + result);
    }

    @Test
    void testGeneratePDFWithEnglishContent() {
        String fileName = "test_english.pdf";
        String content = "This is a test PDF document.\nWith English content.\nTesting font support.";
        
        String result = pdfGenerationTool.generatePDF(fileName, content);
        
        assertNotNull(result);
        assertTrue(result.contains("PDF generated successfully"));
        assertFalse(result.contains("Error"));
        
        System.out.println("PDF Generation Result: " + result);
    }

    @Test
    void testGeneratePDFWithMixedContent() {
        String fileName = "test_mixed.pdf";
        String content = "Mixed Content Test 混合内容测试\n" +
                        "English and Chinese 英文和中文\n" +
                        "Numbers: 123456 数字：123456\n" +
                        "Special chars: !@#$%^&*() 特殊字符：！@#￥%……&*（）";
        
        String result = pdfGenerationTool.generatePDF(fileName, content);
        
        assertNotNull(result);
        assertTrue(result.contains("PDF generated successfully"));
        assertFalse(result.contains("Error"));
        
        System.out.println("PDF Generation Result: " + result);
    }

    @Test
    void testGeneratePDFWithEmptyContent() {
        String fileName = "test_empty.pdf";
        String content = "";
        
        String result = pdfGenerationTool.generatePDF(fileName, content);
        
        assertNotNull(result);
        // 即使内容为空，也应该能成功生成PDF
        assertTrue(result.contains("PDF generated successfully"));
        
        System.out.println("PDF Generation Result: " + result);
    }

    @Test
    void testGeneratePDFWithLongContent() {
        String fileName = "test_long.pdf";
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            content.append("这是第").append(i + 1).append("行内容。This is line ").append(i + 1).append(".\n");
        }
        
        String result = pdfGenerationTool.generatePDF(fileName, content.toString());
        
        assertNotNull(result);
        assertTrue(result.contains("PDF generated successfully"));
        assertFalse(result.contains("Error"));
        
        System.out.println("PDF Generation Result: " + result);
    }
}
