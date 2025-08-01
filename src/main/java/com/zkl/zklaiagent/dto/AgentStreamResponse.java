package com.zkl.zklaiagent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 智能体流式响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentStreamResponse {
    
    /**
     * 消息类型
     */
    private MessageType type;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 当前步骤（可选）
     */
    private Integer currentStep;
    
    /**
     * 总步骤数（可选）
     */
    private Integer totalSteps;
    
    /**
     * 工具调用信息（可选）
     */
    private String toolInfo;
    
    /**
     * 是否完成
     */
    private Boolean finished;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * 消息类型枚举
     */
    public enum MessageType {
        THINKING("思考中"),
        TOOL_CALLING("工具调用"),
        CONTENT("内容输出"),
        STEP_PROGRESS("步骤进展"),
        COMPLETE("完成"),
        ERROR("错误");
        
        private final String description;
        
        MessageType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 创建思考消息
     */
    public static AgentStreamResponse thinking(String content) {
        return AgentStreamResponse.builder()
                .type(MessageType.THINKING)
                .content(content)
                .timestamp(System.currentTimeMillis())
                .finished(false)
                .build();
    }
    
    /**
     * 创建工具调用消息
     */
    public static AgentStreamResponse toolCalling(String toolInfo, String content) {
        return AgentStreamResponse.builder()
                .type(MessageType.TOOL_CALLING)
                .content(content)
                .toolInfo(toolInfo)
                .timestamp(System.currentTimeMillis())
                .finished(false)
                .build();
    }
    
    /**
     * 创建内容输出消息
     */
    public static AgentStreamResponse content(String content) {
        return AgentStreamResponse.builder()
                .type(MessageType.CONTENT)
                .content(content)
                .timestamp(System.currentTimeMillis())
                .finished(false)
                .build();
    }
    
    /**
     * 创建步骤进展消息
     */
    public static AgentStreamResponse stepProgress(int currentStep, int totalSteps, String content) {
        return AgentStreamResponse.builder()
                .type(MessageType.STEP_PROGRESS)
                .content(content)
                .currentStep(currentStep)
                .totalSteps(totalSteps)
                .timestamp(System.currentTimeMillis())
                .finished(false)
                .build();
    }
    
    /**
     * 创建完成消息
     */
    public static AgentStreamResponse complete(String content) {
        return AgentStreamResponse.builder()
                .type(MessageType.COMPLETE)
                .content(content)
                .timestamp(System.currentTimeMillis())
                .finished(true)
                .build();
    }
    
    /**
     * 创建错误消息
     */
    public static AgentStreamResponse error(String content) {
        return AgentStreamResponse.builder()
                .type(MessageType.ERROR)
                .content(content)
                .timestamp(System.currentTimeMillis())
                .finished(true)
                .build();
    }
    
    /**
     * 转换为JSON字符串（用于SSE发送）
     */
    public String toJsonString() {
        return String.format(
            "{\"type\":\"%s\",\"content\":\"%s\",\"currentStep\":%s,\"totalSteps\":%s,\"toolInfo\":\"%s\",\"finished\":%s,\"timestamp\":%d}",
            type.name(),
            escapeJson(content),
            currentStep,
            totalSteps,
            escapeJson(toolInfo),
            finished,
            timestamp
        );
    }
    
    /**
     * JSON字符串转义
     */
    private String escapeJson(String str) {
        if (str == null) return null;
        return str.replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }
}