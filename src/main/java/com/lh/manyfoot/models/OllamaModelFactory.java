package com.lh.manyfoot.models;

import com.lh.manyfoot.domain.AiModelConfig;
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
public class OllamaModelFactory implements AiModelFactory{

    @Override
    public boolean supports(String providerCode) {
         return "Ollama".equalsIgnoreCase(providerCode);
    }

    @Override
    public ChatModel createChatModel(AiModelConfig config) {
        OllamaApi ollamaApi = OllamaApi.builder()
            .baseUrl(config.getBaseUrl())
            .build();

        return OllamaChatModel.builder()
            .ollamaApi(ollamaApi)
            .defaultOptions(
                OllamaChatOptions.builder()
                    .model(config.getModelName())
                    .build())
            .build();
    }

    @Override
    public EmbeddingModel createEmbeddingModel(AiModelConfig config) {
        return OllamaEmbeddingModel.builder()
//           配置Ollama嵌入模型的baseUrl
            .ollamaApi(OllamaApi.builder()
                .baseUrl(config.getBaseUrl())
                .build())


//            指定Ollama模型的名称
            .defaultOptions(OllamaEmbeddingOptions.builder()
                .model(config.getModelName())
                .build())
            .build();
    }

    @Override
    public ImageModel createImageModel(AiModelConfig config) {
        return null;
    }
}
