package com.lh.manyfoot.models;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeImageApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageModel;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageOptions;
import com.lh.manyfoot.domain.AiModelConfig;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.stereotype.Component;

@Component
public class DashscopeModelFactory implements AiModelFactory {

    @Override
    public boolean supports(String providerCode) {
        return "Dashscope".equalsIgnoreCase(providerCode);
    }

    @Override
    public ChatModel createChatModel(AiModelConfig config) {
        return DashScopeChatModel.builder()
            .dashScopeApi(
                DashScopeApi.builder()
                    .apiKey(config.getApiKey())
                    .build()
            )
            .defaultOptions(
                DashScopeChatOptions.builder()
                    .withModel(config.getModelName())
                    .build()
            )
            .build();
    }

    @Override
    public EmbeddingModel createEmbeddingModel(AiModelConfig config) {
        return new DashScopeEmbeddingModel(DashScopeApi.builder().baseUrl(config.getBaseUrl()).build());
    }

    @Override
    public ImageModel createImageModel(AiModelConfig config) {
        return DashScopeImageModel.builder()
            .dashScopeApi(
                DashScopeImageApi.builder()
                    .baseUrl(config.getBaseUrl())
                    .apiKey(config.getApiKey())
                    .build()
            )
            .defaultOptions(
                DashScopeImageOptions.builder()
                    .withModel(config.getModelName())
                    .build()
            )
            .build();
    }

//    private Double getParamAsDouble(AiModelConfig config, String key, Double defaultValue) {
//        return config.getParams().stream()
//            .filter(p -> p.getParamKey().equals(key))
//            .findFirst()
//            .map(p -> Double.parseDouble(p.getParamValue()))
//            .orElse(defaultValue);
//    }
//
//    private Integer getParamAsInteger(AiModelConfig config, String key, Integer defaultValue) {
//        return config.getParams().stream()
//            .filter(p -> p.getParamKey().equals(key))
//            .findFirst()
//            .map(p -> Integer.parseInt(p.getParamValue()))
//            .orElse(defaultValue);
//    }
}
