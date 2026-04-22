package com.lh.manyfoot.config.properties;

import lombok.Getter;

import java.util.Locale;

/**
 * 模型厂商枚举。
 * <p>
 * 每个枚举值的 {@code vendor} 字段即是 YAML 里 {@code many-foot.ai.providers.*.vendor}
 * 使用的 code，也是各 {@code AiModelFactory#supports(String)} 参与匹配的 code。
 */
@Getter
public enum VendorEnums {

    GEMINI("gemini"),
    OPENAI("openai"),
    /**
     * OpenAI 协议兼容的厂商（Moonshot/Kimi、智谱 GLM、SiliconFlow、OpenRouter、
     * Together、Groq、vLLM、LM Studio、DeepSeek /v1、Qwen compatible-mode 等）。
     * 共用 {@code OpenAiCompatibleModelFactory}，仅通过 {@code base-url} 区分。
     */
    OPENAI_COMPATIBLE("openai-compatible"),
    DASHSCOPE("dashscope"),
    OLLAMA("ollama"),
    ANTHROPIC("anthropic"),
    DEEPSEEK("deepseek"),
    QIANFAN("qianfan");

    private final String vendor;

    VendorEnums(String vendor) {
        this.vendor = vendor;
    }

    /**
     * 从字符串 code 反查枚举。大小写不敏感，允许中划线/下划线互换。
     *
     * @param code 原始字符串
     * @return 匹配到的枚举，找不到则抛 {@link IllegalArgumentException}
     */
    public static VendorEnums fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("vendor code 不能为空");
        }
        String normalized = normalize(code);
        for (VendorEnums v : values()) {
            if (normalize(v.vendor).equals(normalized)) {
                return v;
            }
        }
        throw new IllegalArgumentException("未知的模型厂商: " + code
            + "，支持的厂商: gemini/openai/openai-compatible/dashscope/ollama/anthropic/deepseek/qianfan");
    }

    private static String normalize(String s) {
        return s.toLowerCase(Locale.ROOT).replace('_', '-').trim();
    }
}
