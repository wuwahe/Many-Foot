package com.lh.manyfoot.models.registry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * AI模型存储层 - 负责模型实例的内存存储
 * 按角色存储ChatModel实例，支持快速获取
 */
@Component
@Slf4j
public class AiModelStorage {

    private final Map<ModelRole, ChatModel> chatModels = new EnumMap<>(ModelRole.class);

    /**
     * 存储聊天模型
     */
    public void storeChatModel(ModelRole role, ChatModel chatModel) {
        if (role == null || chatModel == null) {
            throw new IllegalArgumentException("模型角色和模型实例不能为空");
        }
        chatModels.put(role, chatModel);
        log.debug("存储聊天模型: role={}", role);
    }

    /**
     * 获取聊天模型
     */
    public Optional<ChatModel> getChatModel(ModelRole role) {
        return Optional.ofNullable(chatModels.get(role));
    }

    /**
     * 获取聊天模型（必须存在，否则抛出异常）
     */
    public ChatModel requireChatModel(ModelRole role) {
        return getChatModel(role).orElseThrow(() ->
            new IllegalStateException("未找到角色[" + role + "]对应的模型，请检查模型是否已注册"));
    }

    /**
     * 移除聊天模型
     */
    public void removeChatModel(ModelRole role) {
        ChatModel removed = chatModels.remove(role);
        if (removed != null) {
            log.debug("移除聊天模型: role={}", role);
        }
    }

    /**
     * 检查模型是否存在
     */
    public boolean containsChatModel(ModelRole role) {
        return chatModels.containsKey(role);
    }

    /**
     * 获取所有已注册的角色
     */
    public Set<ModelRole> getAllRegisteredRoles() {
        return Set.copyOf(chatModels.keySet());
    }

    /**
     * 获取已注册的聊天模型数量
     */
    public int getChatModelCount() {
        return chatModels.size();
    }

    /**
     * 检查是否所有角色都已注册模型
     */
    public boolean isFullyRegistered() {
        return chatModels.size() == ModelRole.values().length;
    }

    /**
     * 清空所有聊天模型
     */
    public void clearAllChatModels() {
        int count = chatModels.size();
        chatModels.clear();
        log.info("清空所有聊天模型，共清除 {} 个", count);
    }
}
