package com.zkl.zklaiagent.tool;

import cn.hutool.core.io.FileUtil;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.zkl.zklaiagent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * PDF 生成工具
 */
public class PDFGenerationTool {

    @Tool(description = "Generate a PDF file with given content", returnDirect = false)
    public String generatePDF(
            @ToolParam(description = "Name of the file to save the generated PDF") String fileName,
            @ToolParam(description = "Content to be included in the PDF") String content) {
        String fileDir = FileConstant.FILE_SAVE_DIR + "/pdf";
        String filePath = fileDir + "/" + fileName;
        try {
            // 创建目录
            FileUtil.mkdir(fileDir);
            // 创建 PdfWriter 和 PdfDocument 对象
            try (PdfWriter writer = new PdfWriter(filePath);
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {

                // 尝试设置中文字体
                PdfFont font = createChineseFont();
                if (font != null) {
                    document.setFont(font);
                }

                // 创建段落
                Paragraph paragraph = new Paragraph(content);
                // 添加段落并关闭文档
                document.add(paragraph);
            }
            return "PDF generated successfully to: " + filePath;
        } catch (IOException e) {
            return "Error generating PDF: " + e.getMessage();
        }
    }

    /**
     * 创建中文字体，尝试多种方案
     */
    private PdfFont createChineseFont() {
        // 方案1：尝试使用系统中文字体
        String[] systemFonts = {
            "C:/Windows/Fonts/simsun.ttc,0",  // Windows 宋体
            "C:/Windows/Fonts/simhei.ttf",    // Windows 黑体
            "C:/Windows/Fonts/msyh.ttc,0",    // Windows 微软雅黑
            "/System/Library/Fonts/PingFang.ttc,0", // macOS 苹方
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf" // Linux
        };

        for (String fontPath : systemFonts) {
            try {
                if (Files.exists(Paths.get(fontPath.split(",")[0]))) {
                    return PdfFontFactory.createFont(fontPath, "Identity-H");
                }
            } catch (Exception e) {
                // 继续尝试下一个字体
            }
        }

        // 方案2：尝试使用iText内置字体
        String[] builtinFonts = {
            "STSongStd-Light",
            "STSong-Light",
            "MSungStd-Light",
            "HeiseiKakuGo-W5"
        };

        for (String fontName : builtinFonts) {
            try {
                return PdfFontFactory.createFont(fontName, "UniGB-UCS2-H");
            } catch (Exception e) {
                // 继续尝试下一个字体
            }
        }

        // 方案3：使用默认字体（可能不支持中文）
        try {
            return PdfFontFactory.createFont();
        } catch (Exception e) {
            // 返回null，使用系统默认字体
            return null;
        }
    }
}
