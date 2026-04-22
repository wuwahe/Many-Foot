package com.lh.manyfoot.agent.core;

import com.lh.manyfoot.agent.prompt.AgentPromptProvider;
import com.lh.manyfoot.agent.strategy.ExecutionStrategy;
import com.lh.manyfoot.agent.tool.AgentToolProvider;
import com.lh.manyfoot.models.registry.ModelResolver;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

/**
 * 支持工具调用的抽象智能体基类
 * 通过AgentToolProvider获取工具列表
 *
 * @param <R> 执行结果类型
 * @author airx
 */
public abstract class AbstractToolAgent<R> extends AbstractAgent<R> implements ToolAwareAgent<R> {

    protected final AgentToolProvider toolProvider;

    protected AbstractToolAgent(ModelResolver modelResolver,
                                AgentPromptProvider promptProvider,
                                ExecutionStrategy<R> executionStrategy,
                                AgentToolProvider toolProvider) {
        super(modelResolver, promptProvider, executionStrategy);
        this.toolProvider = toolProvider;
    }

    @Override
    public List<ToolCallback> getAvailableTools() {
        return toolProvider.getTools();
    }

    @Override
    public boolean requiresTools() {
        return true;
    }

    @Override
    protected List<ToolCallback> getTools() {
        return getAvailableTools();
    }
}
