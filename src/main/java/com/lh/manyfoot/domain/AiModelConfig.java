package com.lh.manyfoot.domain;

import lombok.Data;

/**
 * AI模型配置对象 ai_model_config
 *
 * @author Li
 * @date 2025-11-07
 */
@Data
public class AiModelConfig {

    /**
     * 提供商code
     */
    private String providerCode;

    /**
     * 名字
     */
    private String name;

    /**
     * 模型名称，如: gpt-4, claude-3
     */
    private String modelName;

    /**
     * API密钥（加密存储）
     */
    private String apiKey;

    /**
     * 自定义URL
     */
    private String baseUrl;

    /**
     * 模型类型: CHAT, EMBEDDING, IMAGE, AUDIO
     */
    private String modelType;


}
