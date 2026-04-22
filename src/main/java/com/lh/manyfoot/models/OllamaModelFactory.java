package com.lh.manyfoot.models;

import com.lh.manyfoot.config.properties.VendorEnums;
import com.lh.manyfoot.domain.AiModelConfig;
import com.lh.manyfoot.models.support.ChatOptionsBinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OllamaModelFactory implements AiModelFactory {

    @Override
    public boolean supports(String providerCode) {
        return VendorEnums.OLLAMA.getVendor().equalsIgnoreCase(providerCode);
    }

    @Override
    public ChatModel createChatModel(AiModelConfig config) {
        OllamaApi ollamaApi = buildApi(config);

        OllamaChatOptions options = OllamaChatOptions.builder()
            .model(config.getModelName())
            .build();
        ChatOptionsBinder.bindTemperature(config.getOptions(), options::setTemperature);
        ChatOptionsBinder.bindTopP(config.getOptions(), options::setTopP);
        ChatOptionsBinder.bindTopK(config.getOptions(), options::setTopK);
        ChatOptionsBinder.bindMaxTokens(config.getOptions(), options::setMaxTokens);
        ChatOptionsBinder.bindSeed(config.getOptions(), options::setSeed);
        ChatOptionsBinder.bindStop(config.getOptions(), options::setStop);

        return OllamaChatModel.builder()
            .ollamaApi(ollamaApi)
            .defaultOptions(options)
            .build();
    }

    @Override
    public EmbeddingModel createEmbeddingModel(AiModelConfig config) {
        return OllamaEmbeddingModel.builder()
            .ollamaApi(buildApi(config))
            .defaultOptions(OllamaEmbeddingOptions.builder()
                .model(config.getModelName())
                .build())
            .build();
    }

    @Override
    public ImageModel createImageModel(AiModelConfig config) {
        log.info("Ollama 不提供图像生成（当前版本），跳过 provider=[{}]", config.getId());
        return null;
    }

    private OllamaApi buildApi(AiModelConfig config) {
        OllamaApi.Builder builder = OllamaApi.builder();
        if (config.getBaseUrl() != null && !config.getBaseUrl().isBlank()) {
            builder.baseUrl(config.getBaseUrl());
        }
        return builder.build();
    }
}
