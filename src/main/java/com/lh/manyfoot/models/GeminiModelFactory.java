package com.lh.manyfoot.models;

import com.google.genai.Client;
import com.lh.manyfoot.domain.AiModelConfig;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.image.ImageModel;
import org.springframework.stereotype.Component;

@Component
public class GeminiModelFactory implements AiModelFactory{
    @Override
    public boolean supports(String providerCode) {
        return "Gemini".equalsIgnoreCase(providerCode);
    }

    @Override
    public ChatModel createChatModel(AiModelConfig config) {
        // 创建支持系统代理的 Gemini Client
        Client.Builder clientBuilder = Client.builder()
            .apiKey(config.getApiKey());

        // 配置使用系统的 ProxySelector（支持线程级代理）
        // 注意：Gemini Client 底层使用的 HTTP 客户端会自动使用系统的 ProxySelector

        return GoogleGenAiChatModel.builder()
            .genAiClient(clientBuilder.build())
            .defaultOptions(
                GoogleGenAiChatOptions.builder()
                    .model(config.getModelName())
                    .build()
            )
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
