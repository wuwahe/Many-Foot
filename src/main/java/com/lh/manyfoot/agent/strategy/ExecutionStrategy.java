package com.lh.manyfoot.agent.strategy;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.lh.manyfoot.agent.context.AgentContext;

/**
 * 执行策略接口
 * 使用策略模式封装不同的执行方式（同步/流式）
 *
 * @param <R> 执行返回类型
 * @author airx
 */
public interface ExecutionStrategy<R> {

    /**
     * 执行智能体
     *
     * @param agent   ReactAgent实例
     * @param input   输入内容
     * @param context 执行上下文
     * @return 执行结果
     */
    R execute(ReactAgent agent, String input, AgentContext context);

    /**
     * 获取策略名称
     *
     * @return 策略名称
     */
    String getStrategyName();
}
