package com.zkl.zklaiagent.advisor;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisorChain;
import org.springframework.ai.chat.model.MessageAggregator;
import org.springframework.util.StringUtils;
import com.github.houbb.sensitive.word.core.SensitiveWordHelper;
import com.github.houbb.sensitive.word.bs.SensitiveWordBs;

import java.util.Arrays;
import java.util.List;

/**
 * 违禁词校验 Advisor
 * 检查用户输入和AI响应中是否包含违禁词
 */
@Slf4j
public class ForbiddenWordAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    // 违禁词列表（实际项目中可以从数据库或配置文件加载）
    // 初始化时加载词库
//    private static final List<String> FORBIDDEN_WORDS = loadForbiddenWords();
    private final SensitiveWordBs sensitiveWordBs;

    //    private static List<String> loadForbiddenWords() {
//        // 可以从文件或数据库加载
//        // 这里演示使用开源库提供的词库
//        return SensitiveWordHelper.getDefaultWordList();
//    }
    public ForbiddenWordAdvisor() {
        this.sensitiveWordBs = SensitiveWordBs.newInstance()
                .ignoreCase(true)
                .ignoreWidth(true)
                .init();
    }

    private void checkForbiddenWords(String text, String source) {
        if (sensitiveWordBs.contains(text)) {
            List<String> words = sensitiveWordBs.findAll(text);
            String errorMsg = String.format("%s包含违禁词: %s", source, words);
            log.warn(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
    }


    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return -1; // 设置一个较高的优先级，确保在其他advisor之前执行
    }

    /**
     * 校验请求中的违禁词
     */
    private AdvisedRequest validateRequest(AdvisedRequest request) {
        String userText = request.userText();
        if (StringUtils.hasText(userText)) {
            checkForbiddenWords(userText, "用户输入");
        }
        return request;
    }

    /**
     * 校验响应中的违禁词
     */
    private AdvisedResponse validateResponse(AdvisedResponse response) {
        String aiText = response.response().getResult().getOutput().getText();
        if (StringUtils.hasText(aiText)) {
            checkForbiddenWords(aiText, "AI响应");
        }
        return response;
    }


    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        // 校验请求
        advisedRequest = validateRequest(advisedRequest);

        // 执行后续调用链
        AdvisedResponse advisedResponse = chain.nextAroundCall(advisedRequest);

        // 校验响应
        return validateResponse(advisedResponse);
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        // 校验请求
        advisedRequest = validateRequest(advisedRequest);

        // 执行后续调用链
        Flux<AdvisedResponse> advisedResponses = chain.nextAroundStream(advisedRequest);

        // 使用MessageAggregator聚合流式响应并校验违禁词
        return new MessageAggregator().aggregateAdvisedResponse(advisedResponses, this::validateResponse);
    }
}