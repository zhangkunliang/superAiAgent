package com.zkl.zklaiagent;

import com.zkl.zklaiagent.tool.ImageSearchTool;
import org.springframework.ai.autoconfigure.vectorstore.pgvector.PgVectorStoreAutoConfiguration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class
        ,
        PgVectorStoreAutoConfiguration.class
})public class ZklAiAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZklAiAgentApplication.class, args);
    }

//    @Bean
//    public ToolCallbackProvider imageSearchTools(ImageSearchTool imageSearchTool) {
//        return MethodToolCallbackProvider.builder()
//                .toolObjects(imageSearchTool)
//                .build();
//    }
}
