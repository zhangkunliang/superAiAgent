package com.zkl.zklaiagent.tool;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 集中的工具注册类
 */
@Configuration
public class ToolRegistration {

    @Value("${search-api.api-key}")
    private String searchApiKey;

    @Value("${zhipu-ai.api-key}")
    private String zhipuAiApiKey;

    @Value("${cogview.api-key}")
    private String cogviewApiKey;

    @Value("${dashscope.api-key}")
    private String dashscopeApiKey;

    @Bean
    public ToolCallback[] allTools() {
        FileOperationTool fileOperationTool = new FileOperationTool();
        WebSearchTool webSearchTool = new WebSearchTool(searchApiKey);
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        ResourceDownloadTool resourceDownloadTool = new ResourceDownloadTool();
        TerminalOperationTool terminalOperationTool = new TerminalOperationTool();
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();
        TerminateTool terminateTool = new TerminateTool();
        ZhipuAITool zhipuAITool = new ZhipuAITool(zhipuAiApiKey);
        DashScopeMultiModalTool DashScopeMultiModalTool = new DashScopeMultiModalTool(dashscopeApiKey);
        return ToolCallbacks.from(
                fileOperationTool,
                webSearchTool,
                webScrapingTool,
                resourceDownloadTool,
                terminalOperationTool,
                pdfGenerationTool,
                terminateTool,
                zhipuAITool
                ,
                DashScopeMultiModalTool
        );
    }
}
