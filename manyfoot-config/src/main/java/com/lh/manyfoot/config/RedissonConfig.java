package com.lh.manyfoot.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 配置类
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.data.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.database:0}")
    private int database;

    @Value("${spring.data.redis.timeout:10s}")
    private String timeout;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();

        String address = "redis://" + host + ":" + port;

        SingleServerConfig serverConfig = config.useSingleServer()
            .setAddress(address)
            .setDatabase(database)
            .setConnectTimeout(parseTimeout(timeout));

        // 设置密码（如果有）
        if (password != null && !password.isEmpty()) {
            serverConfig.setPassword(password);
        }

        return Redisson.create(config);
    }

    private int parseTimeout(String timeout) {
        // 解析如 "10s" 格式的超时时间
        if (timeout.endsWith("s")) {
            return Integer.parseInt(timeout.replace("s", "")) * 1000;
        } else if (timeout.endsWith("ms")) {
            return Integer.parseInt(timeout.replace("ms", ""));
        }
        return 10000; // 默认10秒
    }
}
