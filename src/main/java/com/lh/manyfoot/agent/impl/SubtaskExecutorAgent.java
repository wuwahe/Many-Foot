package com.lh.manyfoot.agent.impl;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.lh.manyfoot.agent.context.AgentContext;
import com.lh.manyfoot.agent.core.AbstractToolAgent;
import com.lh.manyfoot.agent.core.StreamingAgent;
import com.lh.manyfoot.agent.prompt.SubtaskExecutorPromptProvider;
import com.lh.manyfoot.agent.strategy.StreamingStrategy;
import com.lh.manyfoot.agent.tool.FullToolProvider;
import com.lh.manyfoot.models.registry.AiModelStorage;
import com.lh.manyfoot.models.registry.ModelRole;
import com.lh.manyfoot.orchestrator.domain.Subtask;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 子任务执行器智能体
 * 针对特定子任务构建专门的执行器
 *
 * 特点：
 * - 使用EXECUTE角色模型
 * - 需要完整CodeAct工具集
 * - 流式输出
 * - 每次执行针对特定子任务，名称动态生成
 *
 * @author airx
 */
@Component
public class SubtaskExecutorAgent extends AbstractToolAgent<Flux<NodeOutput>>
    implements StreamingAgent<NodeOutput> {

    public SubtaskExecutorAgent(AiModelStorage aiModelStorage,
                                SubtaskExecutorPromptProvider promptProvider,
                                FullToolProvider toolProvider) {
        super(aiModelStorage, promptProvider, new StreamingStrategy(), toolProvider);
    }

    @Override
    public String getName() {
        return "Subtask_Executor_agent";
    }

    @Override
    public String getDescription() {
        return "子任务执行智能体，专注于执行特定的子任务";
    }

    @Override
    protected ModelRole getModelRole() {
        return ModelRole.EXECUTE;
    }

    @Override
    public Flux<NodeOutput> stream(AgentContext context) {
        return execute(context);
    }

    /**
     * 获取带子任务ID的名称
     *
     * @param subtask 子任务
     * @return 动态名称
     */
    public String getNameForSubtask(Subtask subtask) {
        return "Subtask_Executor_" + subtask.getId();
    }

    /**
     * 重写构建ReactAgent方法，使用动态名称
     */
    @Override
    protected ReactAgent buildReactAgent(AgentContext context) {
        ChatModel chatModel = getChatModel();
        String systemPrompt = promptProvider.buildSystemPrompt(context);
        List<ToolCallback> tools = getTools();

        // 使用动态名称
        String agentName = context.getCurrentSubtask() != null
            ? getNameForSubtask(context.getCurrentSubtask())
            : getName();

        var builder = ReactAgent.builder()
            .name(agentName)
            .systemPrompt(systemPrompt)
            .model(chatModel);

        if (tools != null && !tools.isEmpty()) {
            builder.tools(tools.toArray(new ToolCallback[0]));
        }

        return builder.build();
    }
}
