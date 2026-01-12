package com.lh.manyfoot.agent.core;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.lh.manyfoot.agent.context.AgentContext;
import com.lh.manyfoot.agent.prompt.AgentPromptProvider;
import com.lh.manyfoot.agent.strategy.ExecutionStrategy;
import com.lh.manyfoot.models.registry.AiModelStorage;
import com.lh.manyfoot.models.registry.ModelRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import java.util.Collections;
import java.util.List;

/**
 * 抽象智能体基类
 * 使用模板方法模式定义智能体执行流程
 *
 * @param <R> 执行结果类型
 * @author airx
 */
@Slf4j
public abstract class AbstractAgent<R> implements Agent<R> {

    protected final AiModelStorage aiModelStorage;
    protected final AgentPromptProvider promptProvider;
    protected final ExecutionStrategy<R> executionStrategy;

    protected AbstractAgent(AiModelStorage aiModelStorage,
                            AgentPromptProvider promptProvider,
                            ExecutionStrategy<R> executionStrategy) {
        this.aiModelStorage = aiModelStorage;
        this.promptProvider = promptProvider;
        this.executionStrategy = executionStrategy;
    }

    /**
     * 模板方法：执行智能体
     * 定义执行流程：构建Agent -> 执行 -> 返回结果
     */
    @Override
    public R execute(AgentContext context) {
        log.info("开始执行智能体: name={}, sessionId={}", getName(), context.getSessionId());

        // 1. 构建ReactAgent
        ReactAgent reactAgent = buildReactAgent(context);

        // 2. 构建输入
        String input = promptProvider.buildUserInput(context);

        // 3. 使用策略执行
        R result = executionStrategy.execute(reactAgent, input, context);

        log.info("智能体执行完成: name={}", getName());
        return result;
    }

    /**
     * 构建ReactAgent实例
     *
     * @param context 执行上下文
     * @return ReactAgent实例
     */
    protected ReactAgent buildReactAgent(AgentContext context) {
        ChatModel chatModel = getChatModel();
        String systemPrompt = promptProvider.buildSystemPrompt(context);
        List<ToolCallback> tools = getTools();

        var builder = ReactAgent.builder()
            .name(getName())
            .systemPrompt(systemPrompt)
            .model(chatModel);

        // 如果有工具则配置工具
        if (tools != null && !tools.isEmpty()) {
            builder.tools(tools.toArray(new ToolCallback[0]));
        }

        return builder.build();
    }

    /**
     * 获取ChatModel
     *
     * @return ChatModel实例
     */
    protected ChatModel getChatModel() {
        return aiModelStorage.requireChatModel(getModelRole());
    }

    // ========== 抽象方法，由子类实现 ==========

    /**
     * 获取模型角色
     *
     * @return 模型角色
     */
    protected abstract ModelRole getModelRole();

    /**
     * 获取工具列表
     * 默认返回空列表，子类可覆盖
     *
     * @return 工具回调列表
     */
    protected List<ToolCallback> getTools() {
        return Collections.emptyList();
    }
}
