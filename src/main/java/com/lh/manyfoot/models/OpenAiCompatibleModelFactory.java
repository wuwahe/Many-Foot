package com.lh.manyfoot.models;

import com.lh.manyfoot.config.properties.VendorEnums;
import com.lh.manyfoot.domain.AiModelConfig;
import com.lh.manyfoot.models.support.ChatOptionsBinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.stereotype.Component;

/**
 * OpenAI 协议兼容厂商通用工厂。
 * <p>
 * 适用于所有遵循 {@code /v1/chat/completions} 与 {@code /v1/embeddings} 接口的厂商。
 * Spring AI OpenAiApi 会自行追加 {@code /v1/chat/completions} 与 {@code /v1/embeddings}，
 * 因此 YAML 中的 {@code base-url} 应配置到版本号之前；若误填了结尾的 {@code /v1}，
 * 本工厂会自动归一化，避免请求变成 {@code /v1/v1/chat/completions}。
 * <p>
 * 典型例子：
 * <ul>
 *     <li>Moonshot/Kimi -> {@code https://api.moonshot.cn}</li>
 *     <li>SiliconFlow -> {@code https://api.siliconflow.cn}</li>
 *     <li>OpenRouter -> {@code https://openrouter.ai/api}</li>
 *     <li>Together -> {@code https://api.together.xyz}</li>
 *     <li>Groq -> {@code https://api.groq.com/openai}</li>
 *     <li>DeepSeek -> {@code https://api.deepseek.com}</li>
 *     <li>Qwen compatible-mode -> {@code https://dashscope.aliyuncs.com/compatible-mode}</li>
 *     <li>vLLM / LM Studio / LocalAI 等自建 -> 配置 local base-url</li>
 * </ul>
 * 新增此类厂商零代码成本，只需在 YAML 里加一个 provider 配置。
 */
@Component
@Slf4j
public class OpenAiCompatibleModelFactory implements AiModelFactory {

    @Override
    public boolean supports(String providerCode) {
        return VendorEnums.OPENAI_COMPATIBLE.getVendor().equalsIgnoreCase(providerCode);
    }

    @Override
    public ChatModel createChatModel(AiModelConfig config) {
        requireBaseUrl(config);

        OpenAiApi openAiApi = OpenAiApi.builder()
            .apiKey(config.getApiKey() == null ? "" : config.getApiKey())
            .baseUrl(normalizeBaseUrlForSpringOpenAi(config.getBaseUrl()))
            .build();

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
        requireBaseUrl(config);

        OpenAiApi openAiApi = OpenAiApi.builder()
            .apiKey(config.getApiKey() == null ? "" : config.getApiKey())
            .baseUrl(normalizeBaseUrlForSpringOpenAi(config.getBaseUrl()))
            .build();

        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
            .model(config.getModelName())
            .build();

        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, options,
            RetryUtils.DEFAULT_RETRY_TEMPLATE);
    }

    @Override
    public ImageModel createImageModel(AiModelConfig config) {
        log.info("厂商 [{}] 未实现图片模型（OpenAI-Compatible 下游差异较大），跳过", config.getId());
        return null;
    }

    private void requireBaseUrl(AiModelConfig config) {
        if (config.getBaseUrl() == null || config.getBaseUrl().isBlank()) {
            throw new IllegalArgumentException(
                "openai-compatible provider [" + config.getId() + "] 必须配置 base-url");
        }
    }

    static String normalizeBaseUrlForSpringOpenAi(String baseUrl) {
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith("/v1")) {
            return normalized.substring(0, normalized.length() - "/v1".length());
        }
        return normalized;
    }
}
