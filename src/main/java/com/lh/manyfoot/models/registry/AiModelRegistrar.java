package com.lh.manyfoot.models.registry;

import com.lh.manyfoot.config.properties.AgentModelProperties;
import com.lh.manyfoot.config.properties.VendorEnums;
import com.lh.manyfoot.domain.AiModelConfig;
import com.lh.manyfoot.models.AiModelFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI模型注册器 - 负责根据配置自动注册模型到存储层
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AiModelRegistrar {

    private final List<AiModelFactory> modelFactories;
    private final AiModelStorage modelStorage;
    private final AgentModelProperties modelProperties;

    /**
     * 根据配置的厂商自动注册所有角色的模型
     */
    public void registerAllModels() {
        VendorEnums vendor = modelProperties.getVendor();
        if (vendor == null) {
            throw new IllegalStateException("未配置模型厂商(many-foot.model.vendor)");
        }

        log.info("开始注册厂商[{}]的所有模型...", vendor);

        // 查找对应的模型工厂
        AiModelFactory factory = findFactory(vendor);

        // 获取该厂商的所有模型配置
        ModelNameEnums[] vendorModels = ModelNameEnums.getByVendor(vendor);

        for (ModelNameEnums modelEnum : vendorModels) {
            try {
                registerModel(factory, modelEnum);
            } catch (Exception e) {
                log.error("注册模型失败: vendor={}, role={}, model={}",
                    vendor, modelEnum.getRole(), modelEnum.getModelName(), e);
                throw new RuntimeException("模型注册失败: " + modelEnum.getModelName(), e);
            }
        }

        log.info("模型注册完成，共注册 {} 个模型", modelStorage.getChatModelCount());
    }

    /**
     * 注册单个模型
     */
    private void registerModel(AiModelFactory factory, ModelNameEnums modelEnum) {
        AiModelConfig config = buildConfig(modelEnum);
        ChatModel chatModel = factory.createChatModel(config);
        modelStorage.storeChatModel(modelEnum.getRole(), chatModel);

        log.info("注册模型成功: role={}, model={}",
            modelEnum.getRole(), modelEnum.getModelName());
    }

    /**
     * 构建模型配置
     */
    private AiModelConfig buildConfig(ModelNameEnums modelEnum) {
        AiModelConfig config = new AiModelConfig();
        config.setProviderCode(modelEnum.getVendor().getVendor());
        config.setModelName(modelEnum.getModelName());
        config.setApiKey(modelProperties.getApiKey());
        config.setBaseUrl(modelProperties.getBaseUrl());
        config.setModelType("CHAT");
        return config;
    }

    /**
     * 查找支持指定厂商的模型工厂
     */
    private AiModelFactory findFactory(VendorEnums vendor) {
        String vendorCode = vendor.getVendor();
        return modelFactories.stream()
            .filter(f -> f.supports(vendorCode))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "未找到厂商[" + vendor + "]对应的模型工厂，请检查是否已实现对应的AiModelFactory"));
    }

}
