package com.lh.manyfoot.domain;

import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * AI 模型运行时配置对象。
 * <p>
 * 由 {@code AiModelRegistrar} 根据 {@code AiProvidersProperties} 中的单个 provider
 * 定义在启动期组装，再交给对应的 {@code AiModelFactory} 创建模型实例。
 *
 * @author Li
 * @date 2025-11-07
 */
@Data
public class AiModelConfig {

    /**
     * provider 的逻辑 id（即 YAML 中 providers 下的 key），模型落入 ModelResolver 时的主键。
     */
    private String id;

    /**
     * 提供商 code，对应 {@link com.lh.manyfoot.config.properties.VendorEnums#getVendor()}。
     */
    private String providerCode;

    /**
     * 可读名称（可选，默认与 id 相同，便于日志）。
     */
    private String name;

    /**
     * 模型名称，例如: gpt-4o / qwen-max / claude-sonnet-4。
     */
    private String modelName;

    /**
     * API 密钥。
     */
    private String apiKey;

    /**
     * 自定义 BaseURL（可选）。
     */
    private String baseUrl;

    /**
     * 模型类型: CHAT / EMBEDDING / IMAGE / AUDIO（当前仅用作日志标记）。
     */
    private String modelType;

    /**
     * 采样等通用参数：temperature / topP / maxTokens / stop / seed /
     * presencePenalty / frequencyPenalty 等，由各 Factory 自行映射。
     */
    private Map<String, Object> options;

    /**
     * 请求超时（毫秒）。
     */
    private Integer timeoutMs;

    /**
     * 附加 HTTP Header（OpenAI 兼容 / Anthropic 版本号等场景可用）。
     */
    private Map<String, String> headers;

    /**
     * 备用 provider id 列表（仅用于日志/校验；实际 failover 由 ModelResolver 组装）。
     */
    private List<String> fallbackIds;

    public Map<String, Object> getOptions() {
        return options == null ? Collections.emptyMap() : options;
    }

    public Map<String, String> getHeaders() {
        return headers == null ? Collections.emptyMap() : headers;
    }

    public List<String> getFallbackIds() {
        return fallbackIds == null ? Collections.emptyList() : fallbackIds;
    }
}
