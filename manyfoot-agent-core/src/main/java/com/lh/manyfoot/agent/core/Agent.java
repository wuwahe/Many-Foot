package com.lh.manyfoot.agent.core;

import com.lh.manyfoot.agent.context.AgentContext;

/**
 * 智能体顶层接口
 * 定义所有智能体的基本行为
 *
 * @param <R> 执行返回类型
 * @author airx
 */
public interface Agent<R> {

    /**
     * 获取智能体名称
     *
     * @return 智能体名称
     */
    String getName();

    /**
     * 获取智能体描述
     *
     * @return 智能体描述
     */
    String getDescription();

    /**
     * 执行智能体任务
     *
     * @param context 执行上下文
     * @return 执行结果
     */
    R execute(AgentContext context);

    /**
     * 是否支持流式输出
     *
     * @return 默认不支持
     */
    default boolean supportsStreaming() {
        return false;
    }
}
