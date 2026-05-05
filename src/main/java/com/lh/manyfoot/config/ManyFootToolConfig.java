package com.lh.manyfoot.config;

import com.lh.manyfoot.tools.CodeActTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ManyFootToolConfig {

    /**
     * 本地 CodeAct 工具提供者
     */
    @Bean
    @ConditionalOnBean(CodeActTool.class)
    public ToolCallbackProvider codeActToolProvider(CodeActTool codeActTool) {
        return MethodToolCallbackProvider
                .builder()
                .toolObjects(codeActTool)
                .build();
    }
}
