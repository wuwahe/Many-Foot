package com.lh.manyfoot;

import com.lh.manyfoot.config.properties.AiProvidersProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AiProvidersProperties.class)
public class ManyFootApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManyFootApplication.class, args);
    }

}
