package com.lh.manyfoot.agent.context;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 智能体执行上下文
 * 封装智能体执行所需的所有输入参数
 *
 * @author airx
 */
@Data
@Builder
public class AgentContext {

    /** 当前智能体所使用的 ChatModel 是否允许接收多模态 Media 输入。 */
    public static final String MULTIMODAL_INPUT_ENABLED_ATTRIBUTE = "multimodalInputEnabled";

    /** Spring AI ToolContext 中用于传递当前沙箱会话 ID 的键。 */
    public static final String TOOL_CONTEXT_SESSION_ID = "sessionId";

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 原始查询/任务描述
     */
    private String query;

    /**
     * 历史观察结果
     */
    private String observations;

    /**
     * 执行器的执行结果（仅Observer使用）
     */
    private String executeResult;

    /**
     * 额外上下文信息
     */
    private String additionalContext;

    /**
     * 扩展属性
     */
    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();

    /**
     * 上传附件元数据。
     * <p>
     * 图片和普通文件在这里被显式区分，避免把所有路径都拼进 query 后让模型猜测。
     */
    @Builder.Default
    private List<AgentAttachment> attachments = new ArrayList<>();

    /**
     * 获取图片附件。
     */
    public List<AgentAttachment> getImageAttachments() {
        if (attachments == null || attachments.isEmpty()) {
            return Collections.emptyList();
        }
        return attachments.stream()
                .filter(AgentAttachment::isImage)
                .toList();
    }

    /**
     * 获取普通文件附件。
     */
    public List<AgentAttachment> getFileAttachments() {
        if (attachments == null || attachments.isEmpty()) {
            return Collections.emptyList();
        }
        return attachments.stream()
                .filter(attachment -> !attachment.isImage())
                .toList();
    }

    /**
     * 是否包含图片附件。
     */
    public boolean hasImageAttachments() {
        return !getImageAttachments().isEmpty();
    }

    /**
     * 当前执行链路是否允许把图片附件作为 Spring AI Media 传给模型。
     */
    public boolean isMultimodalInputEnabled() {
        return Boolean.TRUE.equals(getAttribute(MULTIMODAL_INPUT_ENABLED_ATTRIBUTE));
    }

    /**
     * 设置当前执行链路是否允许把图片附件作为 Spring AI Media 传给模型。
     */
    public void setMultimodalInputEnabled(boolean multimodalInputEnabled) {
        setAttribute(MULTIMODAL_INPUT_ENABLED_ATTRIBUTE, multimodalInputEnabled);
    }

    /**
     * 设置属性
     *
     * @param key   属性键
     * @param value 属性值
     */
    public void setAttribute(String key, Object value) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        attributes.put(key, value);
    }

    /**
     * 获取属性
     *
     * @param key 属性键
     * @param <T> 属性类型
     * @return 属性值
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        if (attributes == null) {
            return null;
        }
        return (T) attributes.get(key);
    }

    /**
     * 获取属性，带默认值
     *
     * @param key          属性键
     * @param defaultValue 默认值
     * @param <T>          属性类型
     * @return 属性值或默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, T defaultValue) {
        if (attributes == null) {
            return defaultValue;
        }
        T value = (T) attributes.get(key);
        return value != null ? value : defaultValue;
    }
}
