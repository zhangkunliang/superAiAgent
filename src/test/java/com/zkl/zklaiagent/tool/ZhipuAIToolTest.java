package com.zkl.zklaiagent.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ZhipuAITool测试类
 * 注意：需要设置环境变量 ZHIPU_AI_API_KEY 才能运行测试
 */
class ZhipuAIToolTest {

    private ZhipuAITool zhipuAITool;
    private static final String TEST_API_KEY = System.getenv("ZHIPU_AI_API_KEY");

    @BeforeEach
    void setUp() {
        // 如果没有设置API Key，使用测试用的假key
        String apiKey = TEST_API_KEY != null ? TEST_API_KEY : "test-api-key";
        zhipuAITool = new ZhipuAITool(apiKey);
    }

    @Test
    void testGetAvailableModels() {
        String[] models = zhipuAITool.getAvailableModels();
        assertNotNull(models);
        assertTrue(models.length > 0);
        assertTrue(java.util.Arrays.asList(models).contains("glm-4-flash"));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "ZHIPU_AI_API_KEY", matches = ".+")
    void testAskZhipuAI() {
        String response = zhipuAITool.askZhipuAI("你好，请简单介绍一下你自己");
        assertNotNull(response);
        assertFalse(response.isEmpty());
        assertFalse(response.startsWith("Error"));
        System.out.println("ZhipuAI Response: " + response);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "ZHIPU_AI_API_KEY", matches = ".+")
    void testChatWithZhipuAI() {
        String response = zhipuAITool.chatWithZhipuAI("什么是人工智能？", "glm-4-flash");
        assertNotNull(response);
        assertFalse(response.isEmpty());
        assertFalse(response.startsWith("Error"));
        System.out.println("ZhipuAI Chat Response: " + response);
    }

    @Test
    void testChatWithInvalidApiKey() {
        ZhipuAITool invalidTool = new ZhipuAITool("invalid-api-key");
        String response = invalidTool.askZhipuAI("测试问题");
        assertNotNull(response);
        assertTrue(response.contains("Error"));
    }

    @Test
    void testChatWithNullModel() {
        String response = zhipuAITool.chatWithZhipuAI("测试问题", null);
        // 应该使用默认模型，不会因为null而报错
        assertNotNull(response);
    }

    @Test
    void testChatWithEmptyModel() {
        String response = zhipuAITool.chatWithZhipuAI("测试问题", "");
        // 应该使用默认模型，不会因为空字符串而报错
        assertNotNull(response);
    }
}
