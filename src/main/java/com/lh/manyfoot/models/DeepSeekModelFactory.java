package com.lh.manyfoot.models;

import com.lh.manyfoot.domain.AiModelConfig;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.stereotype.Component;

@Component
public class DeepSeekModelFactory implements AiModelFactory{
    @Override
    public boolean supports(String providerCode) {
        return "DeepSeek".equals(providerCode);
    }

    @Override
    public ChatModel createChatModel(AiModelConfig config) {

        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
            .baseUrl(config.getBaseUrl())
            .apiKey(config.getApiKey())
            .build();

        DeepSeekChatOptions deepSeekChatOptions = DeepSeekChatOptions.builder()
            .model(config.getModelName())
            .build();

        return DeepSeekChatModel.builder()
            .deepSeekApi(deepSeekApi)
            .defaultOptions(deepSeekChatOptions)
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
