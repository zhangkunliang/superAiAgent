package com.zkl.zklaiagent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 配置静态资源访问和页面路由
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 配置根路径重定向到主页
        registry.addRedirectViewController("/", "/index.html");
        
        // 配置其他页面的直接访问
        registry.addViewController("/chat").setViewName("forward:/ai-chat.html");
        registry.addViewController("/files").setViewName("forward:/pdf-download.html");
        registry.addViewController("/download").setViewName("forward:/pdf-download.html");
    }
}
