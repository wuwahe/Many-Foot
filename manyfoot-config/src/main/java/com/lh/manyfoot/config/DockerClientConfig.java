package com.lh.manyfoot.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.lh.manyfoot.config.properties.SandboxConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.time.Duration;

/**
 * Docker 客户端配置
 *
 * @author airx
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "many-foot.sandbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DockerClientConfig {

    private final SandboxConfig sandboxConfig;

    @Bean
    @Lazy
    public DockerClient dockerClient() {
        // 构建 Docker 客户端配置
        DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder();

        // 如果配置了 Docker Host，使用配置的地址
        if (sandboxConfig.getDockerHost() != null && !sandboxConfig.getDockerHost().isEmpty()) {
            configBuilder.withDockerHost(sandboxConfig.getDockerHost());
        }

        DefaultDockerClientConfig config = configBuilder.build();

        // 构建 HTTP 客户端
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .maxConnections(100)
            .connectionTimeout(Duration.ofSeconds(30))
            .responseTimeout(Duration.ofSeconds(45))
            .build();

        // 创建 Docker 客户端
        DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);

        // 验证连接（仅日志记录，不阻止启动）
        try {
            dockerClient.pingCmd().exec();
            log.info("Docker 客户端连接成功: {}", config.getDockerHost());
        } catch (Exception e) {
            log.warn("Docker 客户端连接失败: {}，沙箱功能可能不可用", e.getMessage());
        }

        return dockerClient;
    }
}
