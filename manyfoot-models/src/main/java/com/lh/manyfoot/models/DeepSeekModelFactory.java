package com.lh.manyfoot.models;

import com.lh.manyfoot.config.properties.VendorEnums;
import com.lh.manyfoot.domain.AiModelConfig;
import com.lh.manyfoot.models.support.ChatOptionsBinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DeepSeekModelFactory implements AiModelFactory {

    @Override
    public boolean supports(String providerCode) {
        return VendorEnums.DEEPSEEK.getVendor().equalsIgnoreCase(providerCode);
    }

    @Override
    public ChatModel createChatModel(AiModelConfig config) {
        DeepSeekApi.Builder apiBuilder = DeepSeekApi.builder()
            .apiKey(config.getApiKey());
        if (config.getBaseUrl() != null && !config.getBaseUrl().isBlank()) {
            apiBuilder.baseUrl(config.getBaseUrl());
        }

        DeepSeekChatOptions options = DeepSeekChatOptions.builder()
            .model(config.getModelName())
            .build();
        ChatOptionsBinder.bindTemperature(config.getOptions(), options::setTemperature);
        ChatOptionsBinder.bindTopP(config.getOptions(), options::setTopP);
        ChatOptionsBinder.bindMaxTokens(config.getOptions(), options::setMaxTokens);
        ChatOptionsBinder.bindPresencePenalty(config.getOptions(), options::setPresencePenalty);
        ChatOptionsBinder.bindFrequencyPenalty(config.getOptions(), options::setFrequencyPenalty);
        ChatOptionsBinder.bindStop(config.getOptions(), options::setStop);

        return DeepSeekChatModel.builder()
            .deepSeekApi(apiBuilder.build())
            .defaultOptions(options)
            .build();
    }

    @Override
    public EmbeddingModel createEmbeddingModel(AiModelConfig config) {
        log.info("DeepSeek 暂不提供 Embedding 能力，跳过 provider=[{}]", config.getId());
        return null;
    }

    @Override
    public ImageModel createImageModel(AiModelConfig config) {
        log.info("DeepSeek 暂不提供图像能力，跳过 provider=[{}]", config.getId());
        return null;
    }
}
