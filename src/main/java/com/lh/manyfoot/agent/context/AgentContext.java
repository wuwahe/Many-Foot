package com.lh.manyfoot.agent.context;

import com.lh.manyfoot.orchestrator.domain.Subtask;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
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
     * 当前子任务（仅SubtaskExecutor使用）
     */
    private Subtask currentSubtask;

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
