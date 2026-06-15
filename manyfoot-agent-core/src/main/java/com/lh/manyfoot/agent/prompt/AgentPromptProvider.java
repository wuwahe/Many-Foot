package com.lh.manyfoot.agent.prompt;

import com.lh.manyfoot.agent.context.AgentContext;

/**
 * 智能体提示词提供者接口
 * 每种智能体可以有自己的提示词实现
 *
 * @author airx
 */
public interface AgentPromptProvider {

    /**
     * 构建系统提示词
     *
     * @param context 智能体上下文
     * @return 系统提示词
     */
    String buildSystemPrompt(AgentContext context);

    /**
     * 构建用户输入
     * 默认返回query，子类可覆盖
     *
     * @param context 智能体上下文
     * @return 用户输入
     */
    default String buildUserInput(AgentContext context) {
        return context.getQuery();
    }
}
