package com.lh.manyfoot.agent.impl;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.lh.manyfoot.agent.context.AgentContext;
import com.lh.manyfoot.agent.core.AbstractToolAgent;
import com.lh.manyfoot.agent.core.Agent;
import com.lh.manyfoot.agent.core.StreamingAgent;
import com.lh.manyfoot.agent.prompt.SupervisorPromptProvider;
import com.lh.manyfoot.agent.registry.AgentRegistry;
import com.lh.manyfoot.agent.strategy.StreamingStrategy;
import com.lh.manyfoot.agent.strategy.SyncCallStrategy;
import com.lh.manyfoot.agent.supervisor.SupervisorToolProvider;
import com.lh.manyfoot.models.registry.ModelRole;
import com.lh.manyfoot.models.registry.ModelResolver;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Supervisor 编排智能体 —— 多智能体协作系统的顶层调度者。
 * <p>
 * <h2>核心职责</h2>
 * <ul>
 *   <li><b>理解用户意图</b>：解析用户的自然语言请求，判断任务复杂度和所需能力</li>
 *   <li><b>任务分解与规划</b>：将复杂任务拆解为可由子智能体独立完成的子任务</li>
 *   <li><b>智能体调度</b>：通过 ReactAgent 的 tool_call 机制，将子任务委派给合适的专业智能体</li>
 *   <li><b>结果综合</b>：收集各子智能体的执行结果，综合整理后输出最终答案</li>
 *   <li><b>代码执行</b>：在必要时调度 CodeAgent 编写、调试和运行代码</li>
 * </ul>
 *
 * <h2>非职责（Supervisor 不做什么）</h2>
 * <ul>
 *   <li>不直接执行具体的领域任务（代码编写、文档生成、数据分析等）</li>
 *   <li>不直接调用外部工具或 API</li>
 *   <li>不持有或管理具体的子智能体实例</li>
 *   <li>不包含 if/else 路由逻辑 —— 路由决策完全由 LLM 通过 tool_call 驱动</li>
 * </ul>
 *
 * <h2>工具调用编排机制</h2>
 * <p>
 * Supervisor 通过 {@link SupervisorToolProvider} 获取工具列表。
 * 该提供者将 {@code AgentRegistry} 中注册的所有非 Supervisor 智能体
 * 包装为 {@code ToolCallback}，使得 Supervisor 的 ReactAgent 可以
 * 像调用普通工具一样调用子智能体。
 * </p>
 * <p>
 * 编排流程完全由 LLM 驱动：
 * <ol>
 *   <li>ReactAgent 接收用户请求和系统提示词</li>
 *   <li>LLM 根据提示词中的编排原则，决定调用哪个子智能体工具</li>
 *   <li>子智能体执行完毕后，结果返回给 ReactAgent</li>
 *   <li>LLM 根据返回结果决定下一步（继续调度、验证、或输出最终答案）</li>
 * </ol>
 * </p>
 *
 * <h2>为什么不持有具体的子智能体？</h2>
 * <ul>
 *   <li><b>开闭原则</b>：新增子智能体只需加 {@code @Component} 注册到 AgentRegistry，
 *       Supervisor 无需任何修改即可自动发现并调度新智能体</li>
 *   <li><b>单一职责</b>：Supervisor 只关心"调度"，不关心子智能体的内部实现</li>
 *   <li><b>避免循环依赖</b>：如果 Supervisor 持有子智能体引用，而子智能体又可能
 *       通过 AgentRegistry 间接引用 Supervisor，会形成循环依赖</li>
 *   <li><b>可测试性</b>：Supervisor 的行为完全由提示词和工具列表决定，
 *       测试时只需替换 SupervisorToolProvider 即可模拟不同的子智能体组合</li>
 * </ul>
 *
 * @author airx
 * @see SupervisorPromptProvider  Supervisor 专用提示词提供者
 * @see SupervisorToolProvider    Supervisor 工具提供者（子智能体 → ToolCallback）
 * @see com.lh.manyfoot.agent.registry.AgentRegistry  智能体注册表
 */
@Component
public class SupervisorAgent extends AbstractToolAgent<Flux<NodeOutput>>
        implements StreamingAgent<NodeOutput> {

    private final AgentRegistry agentRegistry;

    /**
     * 构造器注入。
     * <p>
     * 依赖说明：
     * <ul>
     *   <li>{@link ModelResolver} —— 根据 {@link ModelRole#SUPERVISOR} 解析对应的 ChatModel，
     *       支持 primary + fallbacks 故障转移</li>
     *   <li>{@link SupervisorPromptProvider} —— 动态生成系统提示词，
     *       包含当前可用的子智能体列表和编排原则</li>
     *   <li>{@link SupervisorToolProvider} —— 将子智能体包装为工具列表，
     *       供 ReactAgent 的 LLM 进行 tool_call 调度</li>
     * </ul>
     * <p>
     * 执行策略使用 {@link SyncCallStrategy}，与项目中其他智能体保持一致。
     * Supervisor 的 ReactAgent 内部会自行处理多轮 tool_call 循环，
     * 外部调用方只需等待最终结果。
     *
     * @param modelResolver   模型解析器，按角色获取 ChatModel
     * @param promptProvider  Supervisor 专用提示词提供者
     * @param toolProvider    Supervisor 工具提供者（子智能体工具化）
     */
    public SupervisorAgent(ModelResolver modelResolver,
                           SupervisorPromptProvider promptProvider,
                           SupervisorToolProvider toolProvider,
                           AgentRegistry agentRegistry) {
        super(modelResolver, promptProvider, new StreamingStrategy(), toolProvider);
        this.agentRegistry = agentRegistry;
    }

    /**
     * 返回智能体唯一名称。
     * <p>
     * 该名称在整个系统中必须唯一，用于：
     * <ul>
     *   <li>AgentRegistry 中的注册与查找</li>
     *   <li>SupervisorToolProvider 中排除自身（防止递归调用）</li>
     *   <li>SupervisorPromptProvider 中排除自身（防止提示词中出现自己）</li>
     *   <li>application.yml 中按智能体名覆盖模型配置</li>
     * </ul>
     *
     * @return 固定值 "Supervisor_agent"
     */
    @Override
    public String getName() {
        return "Supervisor_agent";
    }

    /**
     * 返回智能体描述。
     * <p>
     * 该描述会被 {@link SupervisorPromptProvider} 动态拼接到系统提示词中，
     * 帮助 LLM 理解 Supervisor 的定位。同时也会出现在 AgentRegistry 的查询结果中。
     *
     * @return Supervisor 的职责描述
     */
    @Override
    public String getDescription() {
        return "Supervisor 编排智能体，负责理解用户意图、分解复杂任务、调度子智能体协作完成任务，并综合输出最终结果";
    }

    /**
     * 返回模型角色。
     * <p>
     * {@link ModelResolver} 根据此角色从配置中解析对应的 ChatModel。
     * 在 application.yml 的 {@code many-foot.ai.roles.SUPERVISOR} 中
     * 可配置 primary 模型和 fallbacks 列表。
     * <p>
     * Supervisor 作为顶层编排者，通常需要较强的推理和指令遵循能力，
     * 建议配置为高能力模型（如 qwen-max、gpt-4o 等）。
     *
     * @return {@link ModelRole#SUPERVISOR}
     */
    @Override
    protected ModelRole getModelRole() {
        return ModelRole.SUPERVISOR;
    }

    @Override
    public Flux<NodeOutput> stream(AgentContext context) {
        return execute(context);
    }

    @Override
    protected Set<String> getAvailableTools() {
        return agentRegistry.getAllAgents().stream()
                .map(Agent::getName)
                .collect(Collectors.toSet());
    }
}
