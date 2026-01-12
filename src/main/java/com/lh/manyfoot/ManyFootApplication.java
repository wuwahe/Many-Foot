package com.lh.manyfoot;

import com.lh.manyfoot.config.properties.AgentModelProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AgentModelProperties.class)
public class ManyFootApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManyFootApplication.class, args);
    }

}
