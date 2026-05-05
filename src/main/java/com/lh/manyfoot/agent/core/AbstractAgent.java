package com.lh.manyfoot.agent.core;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.lh.manyfoot.agent.context.AgentContext;
import com.lh.manyfoot.agent.prompt.AgentPromptProvider;
import com.lh.manyfoot.agent.strategy.ExecutionStrategy;
import com.lh.manyfoot.models.registry.ModelResolver;
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

    protected final ModelResolver modelResolver;
    protected final AgentPromptProvider promptProvider;
    protected final ExecutionStrategy<R> executionStrategy;

    protected AbstractAgent(ModelResolver modelResolver,
                            AgentPromptProvider promptProvider,
                            ExecutionStrategy<R> executionStrategy) {
        this.modelResolver = modelResolver;
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

        ReactAgent reactAgent = buildReactAgent(context);

        String input = promptProvider.buildUserInput(context);

        R result = executionStrategy.execute(reactAgent, input, context);

        if (log.isDebugEnabled()) {
            String preview = result != null ? result.toString() : "null";
            if (preview.length() > 300) {
                preview = preview.substring(0, 300) + "...";
            }
            log.debug("智能体执行结果: name={}, resultType={}, resultPreview={}",
                    getName(), result != null ? result.getClass().getSimpleName() : "null", preview);
        }
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

        if (tools != null && !tools.isEmpty()) {
            builder.tools(tools.toArray(new ToolCallback[0]));
        }

        return builder.build();
    }

    /**
     * 获取ChatModel。
     * <p>
     * 调用链：优先 {@code many-foot.ai.agents.{agentName}} 覆盖，
     * 命中则返回其 primary+fallbacks 装饰后的 ChatModel；
     * 否则回退到 {@code many-foot.ai.roles.{role}} 绑定。
     */
    protected ChatModel getChatModel() {
        return modelResolver.forAgent(getName(), getModelRole());
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
