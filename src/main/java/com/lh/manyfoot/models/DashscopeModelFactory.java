package com.lh.manyfoot.models;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeImageApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageModel;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageOptions;
import com.lh.manyfoot.config.properties.VendorEnums;
import com.lh.manyfoot.domain.AiModelConfig;
import com.lh.manyfoot.models.support.ChatOptionsBinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class DashscopeModelFactory implements AiModelFactory {

    @Override
    public boolean supports(String providerCode) {
        return VendorEnums.DASHSCOPE.getVendor().equalsIgnoreCase(providerCode);
    }

    @Override
    public ChatModel createChatModel(AiModelConfig config) {
        DashScopeApi dashScopeApi = buildApi(config);

        DashScopeChatOptions options = DashScopeChatOptions.builder()
            .withModel(config.getModelName())
            .build();
        ChatOptionsBinder.bindTemperature(config.getOptions(), options::setTemperature);
        ChatOptionsBinder.bindTopP(config.getOptions(), options::setTopP);
        ChatOptionsBinder.bindTopK(config.getOptions(), options::setTopK);
        ChatOptionsBinder.bindMaxTokens(config.getOptions(), options::setMaxTokens);
        ChatOptionsBinder.bindStop(config.getOptions(), stops -> options.setStop(new ArrayList<Object>(stops)));
        Double repetitionPenalty = ChatOptionsBinder.asDouble(
            config.getOptions(), "repetition-penalty", "repetitionPenalty");
        if (repetitionPenalty != null) {
            options.setRepetitionPenalty(repetitionPenalty);
        }
        if (!config.getHeaders().isEmpty()) {
            options.setHttpHeaders(config.getHeaders());
        }

        return DashScopeChatModel.builder()
            .dashScopeApi(dashScopeApi)
            .defaultOptions(options)
            .build();
    }

    @Override
    public EmbeddingModel createEmbeddingModel(AiModelConfig config) {
        return new DashScopeEmbeddingModel(buildApi(config));
    }

    @Override
    public ImageModel createImageModel(AiModelConfig config) {
        DashScopeImageApi.Builder apiBuilder = DashScopeImageApi.builder()
            .apiKey(config.getApiKey());
        if (config.getBaseUrl() != null && !config.getBaseUrl().isBlank()) {
            apiBuilder.baseUrl(config.getBaseUrl());
        }

        return DashScopeImageModel.builder()
            .dashScopeApi(apiBuilder.build())
            .defaultOptions(DashScopeImageOptions.builder()
                .withModel(config.getModelName())
                .build())
            .build();
    }

    private DashScopeApi buildApi(AiModelConfig config) {
        DashScopeApi.Builder builder = DashScopeApi.builder()
            .apiKey(config.getApiKey());
        if (config.getBaseUrl() != null && !config.getBaseUrl().isBlank()) {
            builder.baseUrl(config.getBaseUrl());
        }
        return builder.build();
    }

    @SuppressWarnings("unused")
    private static List<Object> toObjectList(List<String> stops) {
        return new ArrayList<>(stops);
    }
}
