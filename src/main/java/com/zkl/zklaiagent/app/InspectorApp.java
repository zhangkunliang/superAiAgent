package com.zkl.zklaiagent.app;

import com.zkl.zklaiagent.advisor.MyLoggerAdvisor;
import com.zkl.zklaiagent.rag.InspectorAppRagCustomAdvisorFactory;
import com.zkl.zklaiagent.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class InspectorApp {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = "扮演疾病预防控制与卫生监督领域的资深专家。开场向用户表明疾控监督专家身份，告知可咨询公共卫生、传染病防控、医疗卫生监管等专业问题。" +
            "围绕以下核心领域提供专业解答：\n" +
            "1. 传染病防控：包括疫情监测预警、流行病学调查、应急处置方案等\n" +
            "2. 公共卫生监督：涵盖医疗机构监管、医疗废物处理、饮用水卫生等\n" +
            "3. 卫生法规标准：解读《传染病防治法》《突发公共卫生事件应急条例》等\n" +
            "4. 疾病预防策略：提供疫苗接种、慢性病管理、职业卫生防护等专业指导\n" +
            "5. 卫生应急响应：针对生物恐怖、食物中毒等突发事件的处置建议\n" +
            "要求用户详细描述问题背景、已采取的措施及具体需求，以便给出符合专业规范的解决方案。所有建议必须基于最新国家卫生标准和技术指南。";
    @Autowired
    private Advisor inspectorAppRagCloudAdvisor;

    @Autowired
    private QueryRewriter queryRewriter;

    /**
     * 初始化 ChatClient
     *
     * @param dashscopeChatModel
     */
    public InspectorApp(ChatModel dashscopeChatModel) {
//        // 初始化基于文件的对话记忆
//        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";
//        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);
        // 初始化基于内存的对话记忆
        ChatMemory chatMemory = new InMemoryChatMemory();
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory),
                        // 自定义日志 Advisor，可按需开启
                        new MyLoggerAdvisor()
//                        ,
//                        new ForbiddenWordAdvisor()
//                        // 自定义推理增强 Advisor，可按需开启
//                       ,new ReReadingAdvisor()
                )
                .build();
    }

    /**
     * AI 基础对话（支持多轮对话记忆）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChat(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    record InspectorReport(String title, List<String> suggestions) {

    }
    /**Add commentMore actions
     * AI 基础对话（支持多轮对话记忆，SSE 流式传输）
     *
     * @param message
     * @param chatId
     * @return
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .stream()
                .content();
    }


    /**
     * AI 疾控监督专家报告功能（实战结构化输出）
     *
     * @param message
     * @param chatId
     * @return
     */
    public InspectorReport doChatWithReport(String message, String chatId) {
        InspectorReport inspectorReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成查询结果，标题为{用户名}的查询报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .entity(InspectorReport.class);
        log.info("inspectorReport: {}", inspectorReport);
        return inspectorReport;
    }

    /**
     * Ai疾控监督专家
     */
    @Resource
    private VectorStore inspectorAppVectorStore;
    @Resource
    private VectorStore pgVectorVectorStore;

    public String doChatWithRag(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                // 应用知识库问答
                .advisors(new QuestionAnswerAdvisor(inspectorAppVectorStore))
                .advisors(inspectorAppRagCloudAdvisor)
                // 应用 RAG 检索增强服务（基于 PgVector 向量存储）
//                .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
                // 应用自定义的 RAG 检索增强服务（文档查询器 + 上下文增强器）
//                .advisors(
//                        InspectorAppRagCustomAdvisorFactory.createInspectorAppRagCustomAdvisor(
//                                inspectorAppVectorStore, "卫生监督"
//                        )
//                )
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    public String doChatWithMcp(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .tools(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


}
