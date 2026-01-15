package com.lh.manyfoot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * MCP 客户端配置
 * 为 MCP SSE 连接添加 Authorization header 鉴权
 */
@Configuration
public class McpClientConfig {

    @Value("${spring.ai.dashscope.api-key}")
    private String dashscopeApiKey;

    /**
     * 自定义 WebClient.Builder，为 MCP 客户端请求添加 Authorization header
     * Spring AI MCP Client WebFlux 使用名为 mcpWebClientBuilder 的 bean
     */
    @Bean
    public WebClient.Builder mcpWebClientBuilder() {
        return WebClient.builder()
                .filter(authorizationHeaderFilter());
    }

    /**
     * 创建添加 Authorization header 的过滤器
     */
    private ExchangeFilterFunction authorizationHeaderFilter() {
        return (request, next) -> {
            ClientRequest newRequest = ClientRequest.from(request)
                    .header("Authorization", "Bearer " + dashscopeApiKey)
                    .build();
            return next.exchange(newRequest);
        };
    }
}
