package com.lh.manyfoot.models;

import com.lh.manyfoot.domain.AiModelConfig;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.ImageModel;

public interface AiModelFactory {
    /**
     * 判断是否支持该提供商
     */
    boolean supports(String providerCode);

    /**
     * 创建聊天模型
     */
    ChatModel createChatModel(AiModelConfig config);

    /**
     * 创建嵌入模型
     */
    EmbeddingModel createEmbeddingModel(AiModelConfig config);

    /**
     * 创建图像模型
     */
    ImageModel createImageModel(AiModelConfig config);
}
