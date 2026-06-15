package com.lh.manyfoot.models;

import com.lh.manyfoot.config.properties.VendorEnums;
import com.lh.manyfoot.domain.AiModelConfig;
import com.lh.manyfoot.models.support.ChatOptionsBinder;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiImageApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.stereotype.Component;

/**
 * 官方 OpenAI 厂商工厂。
 * <p>
 * 非 OpenAI 但协议兼容的厂商（Kimi / 智谱 / SiliconFlow / OpenRouter / Together /
 * Groq / vLLM / DeepSeek-v1 / Qwen compatible-mode 等）请使用
 * {@link OpenAiCompatibleModelFactory}。
 */
@Component
public class OpenAiModelFactory implements AiModelFactory {

    @Override
    public boolean supports(String providerCode) {
        return VendorEnums.OPENAI.getVendor().equalsIgnoreCase(providerCode);
    }

    @Override
    public ChatModel createChatModel(AiModelConfig config) {
        OpenAiApi openAiApi = buildApi(config);

        OpenAiChatOptions options = OpenAiChatOptions.builder()
            .model(config.getModelName())
            .build();
        ChatOptionsBinder.bindTemperature(config.getOptions(), options::setTemperature);
        ChatOptionsBinder.bindTopP(config.getOptions(), options::setTopP);
        ChatOptionsBinder.bindMaxTokens(config.getOptions(), options::setMaxTokens);
        ChatOptionsBinder.bindPresencePenalty(config.getOptions(), options::setPresencePenalty);
        ChatOptionsBinder.bindFrequencyPenalty(config.getOptions(), options::setFrequencyPenalty);
        ChatOptionsBinder.bindSeed(config.getOptions(), options::setSeed);
        ChatOptionsBinder.bindStop(config.getOptions(), options::setStop);
        if (!config.getHeaders().isEmpty()) {
            options.setHttpHeaders(config.getHeaders());
        }

        return OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(options)
            .build();
    }

    @Override
    public EmbeddingModel createEmbeddingModel(AiModelConfig config) {
        OpenAiApi openAiApi = buildApi(config);
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
            .model(config.getModelName())
            .build();
        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, options,
            RetryUtils.DEFAULT_RETRY_TEMPLATE);
    }

    @Override
    public ImageModel createImageModel(AiModelConfig config) {
        OpenAiImageApi.Builder apiBuilder = OpenAiImageApi.builder()
            .apiKey(config.getApiKey());
        if (config.getBaseUrl() != null && !config.getBaseUrl().isBlank()) {
            apiBuilder.baseUrl(config.getBaseUrl());
        }

        OpenAiImageOptions options = OpenAiImageOptions.builder()
            .model(config.getModelName())
            .build();

        return new OpenAiImageModel(apiBuilder.build(), options, RetryUtils.DEFAULT_RETRY_TEMPLATE);
    }

    private OpenAiApi buildApi(AiModelConfig config) {
        OpenAiApi.Builder builder = OpenAiApi.builder()
            .apiKey(config.getApiKey());
        if (config.getBaseUrl() != null && !config.getBaseUrl().isBlank()) {
            builder.baseUrl(config.getBaseUrl());
        }
        return builder.build();
    }
}
