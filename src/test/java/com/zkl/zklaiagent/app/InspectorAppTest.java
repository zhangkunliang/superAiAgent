package com.zkl.zklaiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
class InspectorAppTest {

    @Resource
    private InspectorApp inspectorApp;

    @Test
    void testChat() {
        String chatId = UUID.randomUUID().toString();
        // 第一轮
        String message = "你好，我是吴彦祖";
        String answer = inspectorApp.doChat(message, chatId);
        // 第二轮
        message = "我想查询关于专职和兼职疾控监督员的区别";
        answer = inspectorApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 第三轮
        message = "我刚刚查询了什么来着？刚跟你说过，帮我回忆一下";
        answer = inspectorApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我想查询关于专职和兼职疾控监督员的区别";
        InspectorApp.InspectorReport inspectorReport = inspectorApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(inspectorReport);
    }

    @Test
    void doChat() {
    }

    @Test
    void testDoChatWithReport() {
    }

    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "医院用血的相关政策有哪些？多久输送一次？检测标准有哪些";
        String answer =  inspectorApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void testDoChat() {
    }

    @Test
    void testDoChatWithReport1() {
    }

    @Test
    void testDoChatWithRag() {
    }

    @Test
    void doChatWithMcp() {
        String chatId = UUID.randomUUID().toString();
        // 测试地图 MCP
        String message = "帮我搜索一些关于江西省疾控监督执法的现场图片";
        String answer =  inspectorApp.doChatWithMcp(message, chatId);
        Assertions.assertNull(answer);
    }

}
