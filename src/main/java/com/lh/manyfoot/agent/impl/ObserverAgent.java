package com.lh.manyfoot.agent.impl;

import com.lh.manyfoot.agent.core.AbstractToolAgent;
import com.lh.manyfoot.agent.prompt.ObserverPromptProvider;
import com.lh.manyfoot.agent.strategy.SyncCallStrategy;
import com.lh.manyfoot.agent.tool.ReadOnlyToolProvider;
import com.lh.manyfoot.models.registry.ModelResolver;
import com.lh.manyfoot.models.registry.ModelRole;
import org.springframework.stereotype.Component;

/**
 * 观察者智能体
 * 负责分析和验证执行结果
 *
 * 特点：
 * - 使用OBSERVE角色模型
 * - 只需要只读工具（readSandboxFile, listSandboxDirectory）
 * - 同步调用
 *
 * @author airx
 */
@Component
public class ObserverAgent extends AbstractToolAgent<String> {

    public ObserverAgent(ModelResolver modelResolver,
                         ObserverPromptProvider promptProvider,
                         ReadOnlyToolProvider toolProvider) {
        super(modelResolver, promptProvider, new SyncCallStrategy(), toolProvider);
    }

    @Override
    public String getName() {
        return "Observer_agent";
    }

    @Override
    public String getDescription() {
        return "执行结果观察与验证智能体";
    }

    @Override
    protected ModelRole getModelRole() {
        return ModelRole.OBSERVE;
    }
}
