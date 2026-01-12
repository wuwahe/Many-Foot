package com.lh.manyfoot.models.registry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * AI模型初始化器 - 应用启动时自动注册模型
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AiModelInitializer implements ApplicationRunner {

    private final AiModelRegistrar modelRegistrar;

    @Override
    public void run(ApplicationArguments args) {
        log.info("========== 开始初始化AI模型 ==========");
        try {
            modelRegistrar.registerAllModels();
            log.info("========== AI模型初始化完成 ==========");
        } catch (Exception e) {
            log.error("AI模型初始化失败", e);
            throw new RuntimeException("AI模型初始化失败，应用无法启动", e);
        }
    }
}
