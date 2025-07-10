package com.zkl.zklaiagent.tool;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.HashMap;
import java.util.Map;

/**
 * 智谱AI工具类
 * 基于智谱AI GLM-4模型提供AI对话和文本生成功能
 */
public class ZhipuAITool {

    // 智谱AI的API接口地址
    private static final String ZHIPU_API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";

    // 默认使用的模型
    private static final String DEFAULT_MODEL = "glm-4-flash";

    private final String apiKey;

    public ZhipuAITool(String apiKey) {
        this.apiKey = apiKey;
    }

    @Tool(description = "Generate responses or engage in conversations using the GLM-4 model of Zhipu AI. Supports various natural language processing tasks such as text generation, question answering, and content creation")
    public String chatWithZhipuAI(@ToolParam(description = "The message or question to send to Zhipu AI") String message, @ToolParam(description = "The model to use, default is glm-4-flash") String model) {

        // 如果没有指定模型，使用默认模型
        if (model == null || model.trim().isEmpty()) {
            model = DEFAULT_MODEL;
        }

        try {
            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);

            // 构建messages数组
            JSONArray messages = new JSONArray();
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", message);
            messages.add(userMessage);

            requestBody.put("messages", messages);
            requestBody.put("stream", false); // 不使用流式响应
            requestBody.put("max_tokens", 1024); // 最大token数
            requestBody.put("temperature", 0.7); // 温度参数

            // 设置请求头
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + apiKey);
            headers.put("Content-Type", "application/json");

            // 发送POST请求
            String response = HttpUtil.createPost(ZHIPU_API_URL).addHeaders(headers).body(requestBody.toString()).execute().body();

            // 解析响应
            JSONObject responseJson = JSONUtil.parseObj(response);

            // 检查是否有错误
            if (responseJson.containsKey("error")) {
                JSONObject error = responseJson.getJSONObject("error");
                return "Error from Zhipu AI: " + error.getStr("message");
            }

            // 提取AI回复内容
            JSONArray choices = responseJson.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject firstChoice = choices.getJSONObject(0);
                JSONObject messageObj = firstChoice.getJSONObject("message");
                return messageObj.getStr("content");
            }

            return "No response content received from Zhipu AI";

        } catch (Exception e) {
            return "Error calling Zhipu AI: " + e.getMessage();
        }
    }

    @Tool(description = "Use Zhipu AI GLM-4 model with simple message input (uses default model)")
    public String askZhipuAI(@ToolParam(description = "The question or message to ask Zhipu AI") String question) {
        return chatWithZhipuAI(question, DEFAULT_MODEL);
    }

    /**
     * 获取可用的模型列表
     */
    public String[] getAvailableModels() {
        return new String[]{"glm-4-flash", "glm-4", "glm-4-air", "glm-4-airx", "glm-4-long", "glm-4v", "glm-3-turbo"};
    }
}
