package com.lh.manyfoot.models;

import com.lh.manyfoot.config.properties.VendorEnums;
import com.lh.manyfoot.domain.AiModelConfig;
import com.lh.manyfoot.models.support.ChatOptionsBinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AnthropicModelFactory implements AiModelFactory {

    @Override
    public boolean supports(String providerCode) {
        return VendorEnums.ANTHROPIC.getVendor().equalsIgnoreCase(providerCode);
    }

    @Override
    public ChatModel createChatModel(AiModelConfig config) {
        AnthropicApi.Builder apiBuilder = AnthropicApi.builder()
            .apiKey(config.getApiKey());
        if (config.getBaseUrl() != null && !config.getBaseUrl().isBlank()) {
            apiBuilder.baseUrl(config.getBaseUrl());
        }

        AnthropicChatOptions options = AnthropicChatOptions.builder()
            .model(config.getModelName())
            .build();
        ChatOptionsBinder.bindTemperature(config.getOptions(), options::setTemperature);
        ChatOptionsBinder.bindTopP(config.getOptions(), options::setTopP);
        ChatOptionsBinder.bindTopK(config.getOptions(), options::setTopK);
        ChatOptionsBinder.bindMaxTokens(config.getOptions(), options::setMaxTokens);
        ChatOptionsBinder.bindStop(config.getOptions(), options::setStopSequences);
        if (!config.getHeaders().isEmpty()) {
            options.setHttpHeaders(config.getHeaders());
        }

        return AnthropicChatModel.builder()
            .anthropicApi(apiBuilder.build())
            .defaultOptions(options)
            .build();
    }

    @Override
    public EmbeddingModel createEmbeddingModel(AiModelConfig config) {
        log.info("Anthropic 原生不提供 Embedding，跳过 provider=[{}]", config.getId());
        return null;
    }

    @Override
    public ImageModel createImageModel(AiModelConfig config) {
        log.info("Anthropic 原生不提供图像生成，跳过 provider=[{}]", config.getId());
        return null;
    }
}
