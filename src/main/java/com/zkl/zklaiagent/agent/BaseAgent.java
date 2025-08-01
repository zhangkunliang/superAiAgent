package com.zkl.zklaiagent.agent;

import cn.hutool.core.util.StrUtil;
import com.zkl.zklaiagent.agent.model.AgentState;
import com.zkl.zklaiagent.dto.AgentStreamResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.Media;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 抽象基础代理类，用于管理代理状态和执行流程。
 * <p>
 * 提供状态转换、内存管理和基于步骤的执行循环的基础功能。
 * 子类必须实现step方法。
 */
@Data
@Slf4j
public abstract class BaseAgent {

    // 核心属性
    private String name;

    // 提示词
    private String systemPrompt;
    private String nextStepPrompt;

    // 代理状态
    private AgentState state = AgentState.IDLE;

    // 执行步骤控制
    private int currentStep = 0;
    private int maxSteps = 10;

    // LLM 大模型
    private ChatClient chatClient;

    // Memory 记忆（需要自主维护会话上下文）
    private List<Message> messageList = new ArrayList<>();
    
    // SSE发送器（用于子类发送消息）
    private SseEmitter currentSseEmitter;

    /**
     * 运行代理
     *
     * @param userPrompt 用户提示词
     * @return 执行结果
     */
    public String run(String userPrompt) {
        // 1、基础校验
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("Cannot run agent from state: " + this.state);
        }
        if (StrUtil.isBlank(userPrompt)) {
            throw new RuntimeException("Cannot run agent with empty user prompt");
        }
        // 2、执行，更改状态
        this.state = AgentState.RUNNING;
        // 记录消息上下文
        messageList.add(new UserMessage(userPrompt));
        // 保存结果列表
        List<String> results = new ArrayList<>();
        try {
            // 执行循环
            for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("Executing step {}/{}", stepNumber, maxSteps);
                // 单步执行
                String stepResult = step();
                String result = "Step " + stepNumber + ": " + stepResult;
                results.add(result);
            }
            // 检查是否超出步骤限制
            if (currentStep >= maxSteps) {
                state = AgentState.FINISHED;
                results.add("Terminated: Reached max steps (" + maxSteps + ")");
            }
            return String.join("\n", results);
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("error executing agent", e);
            return "执行错误" + e.getMessage();
        } finally {
            // 3、清理资源
            this.cleanup();
        }
    }

    /**
     * 运行代理（流式输出）
     *
     * @param userPrompt 用户提示词
     * @return 执行结果
     */
//    public SseEmitter runStream(String userPrompt) {
//        // 创建一个超时时间较长的 SseEmitter
//        SseEmitter sseEmitter = new SseEmitter(300000L); // 5 分钟超时
//        // 使用线程异步处理，避免阻塞主线程
//        CompletableFuture.runAsync(() -> {
//            // 1、基础校验
//            try {
//                if (this.state != AgentState.IDLE) {
//                    sseEmitter.send("错误：无法从状态运行代理：" + this.state);
//                    sseEmitter.complete();
//                    return;
//                }
//                if (StrUtil.isBlank(userPrompt)) {
//                    sseEmitter.send("错误：不能使用空提示词运行代理");
//                    sseEmitter.complete();
//                    return;
//                }
//            } catch (Exception e) {
//                sseEmitter.completeWithError(e);
//            }
//            // 2、执行，更改状态
//            this.state = AgentState.RUNNING;
//            // 记录消息上下文
//            messageList.add(new UserMessage(userPrompt));
//            // 保存结果列表
//            List<String> results = new ArrayList<>();
//            try {
//                // 执行循环
//                for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
//                    int stepNumber = i + 1;
//                    currentStep = stepNumber;
//                    log.info("Executing step {}/{}", stepNumber, maxSteps);
//                    // 单步执行
//                    String stepResult = step();
//                    String result = "Step " + stepNumber + ": " + stepResult;
//                    results.add(result);
//                    // 输出当前每一步的结果到 SSE
//                    sseEmitter.send(result);
//                }
//                // 检查是否超出步骤限制
//                if (currentStep >= maxSteps) {
//                    state = AgentState.FINISHED;
//                    results.add("Terminated: Reached max steps (" + maxSteps + ")");
//                    sseEmitter.send("执行结束：达到最大步骤（" + maxSteps + "）");
//                }
//                // 正常完成
//                sseEmitter.complete();
//            } catch (Exception e) {
//                state = AgentState.ERROR;
//                log.error("error executing agent", e);
//                try {
//                    sseEmitter.send("执行错误：" + e.getMessage());
//                    sseEmitter.complete();
//                } catch (IOException ex) {
//                    sseEmitter.completeWithError(ex);
//                }
//            } finally {
//                // 3、清理资源
//                this.cleanup();
//            }
//        });
//
//        // 设置超时回调
//        sseEmitter.onTimeout(() -> {
//            this.state = AgentState.ERROR;
//            this.cleanup();
//            log.warn("SSE connection timeout");
//        });
//        // 设置完成回调
//        sseEmitter.onCompletion(() -> {
//            if (this.state == AgentState.RUNNING) {
//                this.state = AgentState.FINISHED;
//            }
//            this.cleanup();
//            log.info("SSE connection completed");
//        });
//        return sseEmitter;
//    }

//    /**
//     * 运行代理（流式输出）
//     *
//     * @param userPrompt 用户提示词
//     * @return 执行结果
//     */
//    public SseEmitter runStream(String userPrompt) {
//        return runStreamWithImage(userPrompt, null);
//    }
    /**
     * 运行代理（流式输出，支持图片）
     *
     * @param userPrompt 用户提示词
     * @param imageBase64 Base64编码的图片数据（可为null）
     * @return 执行结果
     */
    public SseEmitter runStreamWithImage(String userPrompt, String imageBase64) {
        // 创建一个超时时间较长的 SseEmitter
        SseEmitter sseEmitter = new SseEmitter(300000L); // 5 分钟超时
        
        // 使用线程异步处理，避免阻塞主线程
        CompletableFuture.runAsync(() -> {
            // 设置当前SSE发送器，供子类使用
            this.currentSseEmitter = sseEmitter;
            StringBuilder fullResponse = new StringBuilder();
            
            try {
                // 1、基础校验
                if (this.state != AgentState.IDLE) {
                    AgentStreamResponse errorResponse = AgentStreamResponse.error("无法从当前状态运行代理：" + this.state);
                    sseEmitter.send(errorResponse.toJsonString());
                    sseEmitter.complete();
                    return;
                }
                if (StrUtil.isBlank(userPrompt) && StrUtil.isBlank(imageBase64)) {
                    AgentStreamResponse errorResponse = AgentStreamResponse.error("不能使用空提示词和空图片运行代理");
                    sseEmitter.send(errorResponse.toJsonString());
                    sseEmitter.complete();
                    return;
                }

                // 2、发送开始思考消息
                AgentStreamResponse thinkingResponse = AgentStreamResponse.thinking("🤔 正在分析您的请求，制定执行计划...");
                sseEmitter.send(thinkingResponse.toJsonString());
                fullResponse.append(thinkingResponse.getContent()).append("\n\n");

                // 3、执行，更改状态
                this.state = AgentState.RUNNING;
                
                // 记录消息上下文
                if (imageBase64 != null && !imageBase64.isEmpty()) {
                    // 创建包含图片的用户消息
                    Map<String, Object> mediaMap = new HashMap<>();
                    mediaMap.put("type", "image");
                    mediaMap.put("data", imageBase64);

                    // 使用带有媒体属性的Map创建UserMessage
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("media", List.of(mediaMap));
                    messageList.add(new UserMessage(userPrompt, (List<Media>) metadata));
                } else {
                    // 普通文本消息
                    messageList.add(new UserMessage(userPrompt));
                }

                // 4、执行循环
                for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                    int stepNumber = i + 1;
                    currentStep = stepNumber;
                    
                    log.info("Executing step {}/{}", stepNumber, maxSteps);
                    
                    // 发送步骤开始消息
                    AgentStreamResponse stepStart = AgentStreamResponse.stepProgress(
                        stepNumber, maxSteps, String.format("🚀 开始执行第 %d 步...", stepNumber)
                    );
                    sseEmitter.send(stepStart.toJsonString());
                    
                    // 单步执行
                    String stepResult = step();
                    
                    // 发送步骤内容
                    if (StrUtil.isNotBlank(stepResult)) {
                        AgentStreamResponse contentResponse = AgentStreamResponse.content(stepResult);
                        sseEmitter.send(contentResponse.toJsonString());
                        fullResponse.append(stepResult);
                        
                        // 如果内容不以换行结束，添加两个换行
                        if (!stepResult.endsWith("\n")) {
                            fullResponse.append("\n\n");
                        } else if (!stepResult.endsWith("\n\n")) {
                            fullResponse.append("\n");
                        }
                    }
                    
                    // 短暂延迟，让前端有时间处理消息
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                
                // 5、检查是否超出步骤限制
                if (currentStep >= maxSteps) {
                    state = AgentState.FINISHED;
                    AgentStreamResponse maxStepsResponse = AgentStreamResponse.complete(
                        String.format("⚠️ 已达到最大执行步骤限制（%d 步），任务执行结束。", maxSteps)
                    );
                    sseEmitter.send(maxStepsResponse.toJsonString());
                } else {
                    // 正常完成
                    AgentStreamResponse completeResponse = AgentStreamResponse.complete(
                        "✅ 任务执行完成！感谢您的耐心等待。"
                    );
                    sseEmitter.send(completeResponse.toJsonString());
                }
                
                // 完成SSE连接
                sseEmitter.complete();
                
            } catch (Exception e) {
                state = AgentState.ERROR;
                log.error("Agent execution error", e);
                try {
                    AgentStreamResponse errorResponse = AgentStreamResponse.error(
                        "执行过程中发生错误：" + e.getMessage()
                    );
                    sseEmitter.send(errorResponse.toJsonString());
                    sseEmitter.complete();
                } catch (IOException ex) {
                    sseEmitter.completeWithError(ex);
                }
            } finally {
                // 清理资源
                this.cleanup();
            }
        });

        // 设置超时回调
        sseEmitter.onTimeout(() -> {
            this.state = AgentState.ERROR;
            this.cleanup();
            log.warn("SSE connection timeout");
        });
        
        // 设置完成回调
        sseEmitter.onCompletion(() -> {
            if (this.state == AgentState.RUNNING) {
                this.state = AgentState.FINISHED;
            }
            this.cleanup();
            log.info("SSE connection completed");
        });
        
        return sseEmitter;
    }

    /**
     * 定义单个步骤
     *
     * @return
     */
    public abstract String step();

    /**
     * 清理资源
     */
    protected void cleanup() {
        // 清理SSE发送器
        this.currentSseEmitter = null;
        // 子类可以重写此方法来清理资源
    }
    
    /**
     * 发送SSE消息（供子类使用）
     */
    protected void sendSseMessage(AgentStreamResponse response) {
        if (currentSseEmitter != null) {
            try {
                currentSseEmitter.send(response.toJsonString());
            } catch (IOException e) {
                log.error("Failed to send SSE message", e);
            }
        }
    }
    
    /**
     * 发送思考消息
     */
    protected void sendThinkingMessage(String content) {
        sendSseMessage(AgentStreamResponse.thinking(content));
    }
    
    /**
     * 发送工具调用消息
     */
    protected void sendToolCallingMessage(String toolInfo, String content) {
        sendSseMessage(AgentStreamResponse.toolCalling(toolInfo, content));
    }
    
    /**
     * 发送内容消息
     */
    protected void sendContentMessage(String content) {
        sendSseMessage(AgentStreamResponse.content(content));
    }
}
