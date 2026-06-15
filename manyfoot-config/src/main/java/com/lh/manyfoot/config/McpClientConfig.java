package com.lh.manyfoot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;

@Configuration
@ConditionalOnProperty(prefix = "spring.ai.mcp.client", name = "enabled", havingValue = "true", matchIfMissing = true)
public class McpClientConfig {

    @Value("${spring.ai.dashscope.api-key:}")
    private String dashscopeApiKey;

    @Bean
    public WebClientCustomizer mcpAuthCustomizer() {
        return builder -> builder.filter(
                (request, next) -> {
                    if (dashscopeApiKey == null || dashscopeApiKey.isEmpty()) {
                        return next.exchange(request);
                    }
                    ClientRequest authed = ClientRequest.from(request)
                            .header("Authorization", "Bearer " + dashscopeApiKey)
                            .build();
                    return next.exchange(authed);
                }
        );
    }
}
