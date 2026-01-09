package com.lh.manyfoot.config;

import com.lh.manyfoot.codeact.tools.CodeActTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ManusToolConfig {

    @Bean
    @Primary
    public ToolCallbackProvider salesRecommendTools(CodeActTool codeActTool) {
        return MethodToolCallbackProvider
            .builder()
            .toolObjects(
                codeActTool
            )
            .build();
    }
}
