package com.lh.manyfoot.models;

import com.google.genai.Client;
import com.lh.manyfoot.config.properties.VendorEnums;
import com.lh.manyfoot.domain.AiModelConfig;
import com.lh.manyfoot.models.support.ChatOptionsBinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.image.ImageModel;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GeminiModelFactory implements AiModelFactory {

    @Override
    public boolean supports(String providerCode) {
        return VendorEnums.GEMINI.getVendor().equalsIgnoreCase(providerCode);
    }

    @Override
    public ChatModel createChatModel(AiModelConfig config) {
        Client client = Client.builder()
            .apiKey(config.getApiKey())
            .build();

        GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
            .model(config.getModelName())
            .build();
        ChatOptionsBinder.bindTemperature(config.getOptions(), options::setTemperature);
        ChatOptionsBinder.bindTopP(config.getOptions(), options::setTopP);
        ChatOptionsBinder.bindTopK(config.getOptions(), options::setTopK);
        ChatOptionsBinder.bindMaxTokens(config.getOptions(), options::setMaxTokens);
        ChatOptionsBinder.bindStop(config.getOptions(), options::setStopSequences);
        ChatOptionsBinder.bindFrequencyPenalty(config.getOptions(), options::setFrequencyPenalty);
        ChatOptionsBinder.bindPresencePenalty(config.getOptions(), options::setPresencePenalty);

        return GoogleGenAiChatModel.builder()
            .genAiClient(client)
            .defaultOptions(options)
            .build();
    }

    @Override
    public EmbeddingModel createEmbeddingModel(AiModelConfig config) {
        log.info("Gemini Embedding 暂未接入（需要 google-genai-embedding 额外依赖），跳过 provider=[{}]", config.getId());
        return null;
    }

    @Override
    public ImageModel createImageModel(AiModelConfig config) {
        log.info("Gemini 图像生成暂未接入，跳过 provider=[{}]", config.getId());
        return null;
    }
}
