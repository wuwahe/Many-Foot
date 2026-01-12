package com.lh.manyfoot.models.registry;

import com.lh.manyfoot.config.properties.VendorEnums;
import lombok.Getter;

/**
 * 模型名称枚举 - 按厂商和角色分组
 * 每个枚举值代表一个可用的模型，包含其所属厂商、适用角色和实际模型名称
 */
@Getter
public enum ModelNameEnums {

    // ========== DASHSCOPE 模型 ==========
    DASHSCOPE_ANALYZE(VendorEnums.DASHSCOPE, ModelRole.ANALYZE, "qwen-max"),
    DASHSCOPE_EXECUTE(VendorEnums.DASHSCOPE, ModelRole.EXECUTE, "qwen3-coder-plus"),
    DASHSCOPE_OBSERVE(VendorEnums.DASHSCOPE, ModelRole.OBSERVE, "qwen-plus"),

    // ========== OPENAI 模型 ==========
    OPENAI_ANALYZE(VendorEnums.OPENAI, ModelRole.ANALYZE, "gpt-4o"),
    OPENAI_EXECUTE(VendorEnums.OPENAI, ModelRole.EXECUTE, "gpt-4o-mini"),
    OPENAI_OBSERVE(VendorEnums.OPENAI, ModelRole.OBSERVE, "gpt-4o-mini"),

    // ========== GEMINI 模型 ==========
    GEMINI_ANALYZE(VendorEnums.GEMINI, ModelRole.ANALYZE, "gemini-2.5-pro"),
    GEMINI_EXECUTE(VendorEnums.GEMINI, ModelRole.EXECUTE, "gemini-2.5-pro"),
    GEMINI_OBSERVE(VendorEnums.GEMINI, ModelRole.OBSERVE, "gemini-2.5-flash"),

    // ========== OLLAMA 模型 ==========
    OLLAMA_ANALYZE(VendorEnums.OLLAMA, ModelRole.ANALYZE, "qwen2.5:14b"),
    OLLAMA_EXECUTE(VendorEnums.OLLAMA, ModelRole.EXECUTE, "qwen2.5:7b"),
    OLLAMA_OBSERVE(VendorEnums.OLLAMA, ModelRole.OBSERVE, "qwen2.5:3b");

    /**
     * 厂商
     */
    private final VendorEnums vendor;
    /**
     * 角色
     */
    private final ModelRole role;
    /**
     * 模型名称
     */
    private final String modelName;

    ModelNameEnums(VendorEnums vendor, ModelRole role, String modelName) {
        this.vendor = vendor;
        this.role = role;
        this.modelName = modelName;
    }

    /**
     * 根据厂商和角色查找对应的模型
     */
    public static ModelNameEnums findByVendorAndRole(VendorEnums vendor, ModelRole role) {
        for (ModelNameEnums model : values()) {
            if (model.vendor == vendor && model.role == role) {
                return model;
            }
        }
        throw new IllegalArgumentException(
            String.format("未找到厂商[%s]角色[%s]对应的模型配置", vendor, role));
    }

    /**
     * 获取指定厂商的所有模型
     */
    public static ModelNameEnums[] getByVendor(VendorEnums vendor) {
        return java.util.Arrays.stream(values())
            .filter(m -> m.vendor == vendor)
            .toArray(ModelNameEnums[]::new);
    }
}
