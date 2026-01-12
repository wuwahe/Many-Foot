package com.lh.manyfoot.models;

import com.lh.manyfoot.domain.AiModelConfig;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.stereotype.Component;

@Component
public class AnthropicModelFactory implements AiModelFactory{
    @Override
    public boolean supports(String providerCode) {
        return "Anthropic".equalsIgnoreCase(providerCode);
    }

    @Override
    public ChatModel createChatModel(AiModelConfig config) {
        AnthropicApi anthropicApi = AnthropicApi.builder()
            .apiKey(config.getApiKey())
            .baseUrl(config.getBaseUrl())
            .build();

        AnthropicChatOptions anthropicChatOptions = AnthropicChatOptions.builder()
            .model(config.getModelName())
            .build();

        return AnthropicChatModel.builder()
            .anthropicApi(anthropicApi)
            .defaultOptions(anthropicChatOptions)
            .build();
    }

    @Override
    public EmbeddingModel createEmbeddingModel(AiModelConfig config) {
        return null;
    }

    @Override
    public ImageModel createImageModel(AiModelConfig config) {
        return null;
    }
}
