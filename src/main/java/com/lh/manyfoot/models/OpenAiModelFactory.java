package com.lh.manyfoot.models;

import com.lh.manyfoot.domain.AiModelConfig;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.openai.*;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiImageApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.stereotype.Component;


@Component
public class OpenAiModelFactory implements AiModelFactory{
    @Override
    public boolean supports(String providerCode) {
        return "OpenAi".equals(providerCode);
    }

    @Override
    public ChatModel createChatModel(AiModelConfig config) {
        OpenAiApi openAiApi = OpenAiApi.builder()
            .apiKey(config.getApiKey())
            .baseUrl(config.getBaseUrl())
            .build();

        OpenAiChatOptions openAiChatOptions = OpenAiChatOptions.builder()
            .model(config.getModelName())
            .build();

        return OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(openAiChatOptions)
            .build();
    }

    @Override
    public EmbeddingModel createEmbeddingModel(AiModelConfig config) {

//        配置openai嵌入模型的baseUrl以及apikey
        OpenAiApi openAiApi = OpenAiApi.builder()
            .baseUrl(config.getBaseUrl())
            .apiKey(config.getApiKey())
            .build();
//      配置openai嵌入模型使用的模型
        OpenAiEmbeddingOptions openAiEmbeddingOptions = OpenAiEmbeddingOptions.builder()
            .model(config.getModelName())
            .build();

        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, openAiEmbeddingOptions, RetryUtils.DEFAULT_RETRY_TEMPLATE);
    }

    @Override
    public ImageModel createImageModel(AiModelConfig config) {

//      创建openai的图片模型，指定apikey以及baseUrl
        OpenAiImageApi openAiImageApi = OpenAiImageApi.builder()
            .apiKey(config.getApiKey())
            .baseUrl(config.getBaseUrl())
            .build();

//      设定图片模型使用的模型
        OpenAiImageOptions openAiImageOptions = OpenAiImageOptions.builder()
            .model(config.getModelName())
            .build();

        return new OpenAiImageModel(openAiImageApi, openAiImageOptions,RetryUtils.DEFAULT_RETRY_TEMPLATE);
    }
}
