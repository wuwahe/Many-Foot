package com.lh.manyfoot.config;

import com.lh.manyfoot.agent.tool.sandbox.SandboxTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ManyFootToolConfig {

    /**
     * 本地沙箱工具提供者
     */
    @Bean
    @ConditionalOnBean(SandboxTool.class)
    public ToolCallbackProvider sandboxToolProvider(SandboxTool sandboxTool) {
        return MethodToolCallbackProvider
                .builder()
                .toolObjects(sandboxTool)
                .build();
    }
}
