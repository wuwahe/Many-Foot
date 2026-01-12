package com.lh.manyfoot.agent.impl;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.lh.manyfoot.agent.context.AgentContext;
import com.lh.manyfoot.agent.core.AbstractToolAgent;
import com.lh.manyfoot.agent.core.StreamingAgent;
import com.lh.manyfoot.agent.prompt.ExecutorPromptProvider;
import com.lh.manyfoot.agent.strategy.StreamingStrategy;
import com.lh.manyfoot.agent.tool.FullToolProvider;
import com.lh.manyfoot.models.registry.AiModelStorage;
import com.lh.manyfoot.models.registry.ModelRole;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 执行器智能体
 * 负责执行具体的代码任务
 *
 * 特点：
 * - 使用EXECUTE角色模型
 * - 需要完整CodeAct工具集
 * - 流式输出
 *
 * @author airx
 */
@Component
public class ExecutorAgent extends AbstractToolAgent<Flux<NodeOutput>>
    implements StreamingAgent<NodeOutput> {

    public ExecutorAgent(AiModelStorage aiModelStorage,
                         ExecutorPromptProvider promptProvider,
                         FullToolProvider toolProvider) {
        super(aiModelStorage, promptProvider, new StreamingStrategy(), toolProvider);
    }

    @Override
    public String getName() {
        return "Executor_agent";
    }

    @Override
    public String getDescription() {
        return "代码执行智能体，在沙箱环境中执行代码完成任务";
    }

    @Override
    protected ModelRole getModelRole() {
        return ModelRole.EXECUTE;
    }

    @Override
    public Flux<NodeOutput> stream(AgentContext context) {
        return execute(context);
    }
}
