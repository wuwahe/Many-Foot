package com.lh.manyfoot.agent.supervisor;

import com.lh.manyfoot.agent.core.Agent;
import com.lh.manyfoot.agent.registry.AgentRegistry;
import com.lh.manyfoot.agent.tool.AgentToolProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Supervisor 工具提供者
 * <p>
 * 将 {@link AgentRegistry} 中注册的所有<b>非 Supervisor</b>智能体
 * 包装为 {@link ToolCallback}，供 Supervisor 在编排过程中当作工具调用。
 * <p>
 * <h3>为什么需要这个类？</h3>
 * Supervisor 本身不执行具体任务，而是通过工具调用的方式委派给子智能体。
 * 本类的职责就是把"子智能体列表"转换为"工具列表"，让 Supervisor 的
 * ReactAgent 可以像调用普通工具一样调用子智能体。
 * <p>
 * <h3>循环依赖防护（关键设计）</h3>
 * Spring 容器中存在以下 Bean 依赖链：
 * <pre>
 *   AgentRegistry(List&lt;Agent&lt;String&gt;&gt;) → SupervisorAgent → SupervisorToolProvider → AgentRegistry
 * </pre>
 * 如果 SupervisorToolProvider 在构造期就急切地访问 AgentRegistry，
 * 会触发上述循环，导致 Bean 创建失败。
 * <p>
 * <b>解决方案：</b>使用 {@code @Lazy} 注入 AgentRegistry。
 * Spring 会注入一个延迟代理（CGLIB proxy），真正的 AgentRegistry 实例
 * 直到 {@link #getTools()} 被首次调用时才会解析。
 * 此时所有 Bean 已完成初始化，循环依赖不再存在。
 * <p>
 * <h3>递归调用防护</h3>
 * AgentRegistry 会收集容器中<b>所有</b> {@code Agent<String>} Bean，
 * 包括 SupervisorAgent 自身。如果 Supervisor 的工具列表中包含自己，
 * 就会导致无限递归调用。
 * <p>
 * <b>解决方案：</b>通过 {@link AgentRegistry#getAgentsExcluding(String...)}
 * 排除名为 "Supervisor_agent" 的智能体，确保 Supervisor 不会调用自己。
 * <p>
 * <h3>为什么不注入具体 Agent？</h3>
 * <ul>
 *   <li>注入具体 Agent 会导致与特定实现的耦合，违反开闭原则</li>
 *   <li>新增子智能体时只需加 {@code @Component}，无需修改本类</li>
 *   <li>AgentRegistry 提供了统一的发现和过滤能力</li>
 * </ul>
 * <p>
 * <h3>为什么不注入 AgentFactory？</h3>
 * AgentFactory 是兼容性门面，采用硬编码的 switch-case 模式。
 * 本类使用 AgentRegistry 的动态发现能力，与 AgentFactory 无关。
 *
 * @author airx
 * @see AgentToolProvider
 * @see AgentRegistry
 * @see AgentTool
 */
@Slf4j
@Component
public class SupervisorToolProvider implements AgentToolProvider {

    /**
     * Supervisor 智能体的名称常量。
     * <p>
     * 必须与 SupervisorAgent.getName() 的返回值完全一致。
     * 用于从工具列表中排除 Supervisor 自身，防止递归调用。
     */
    private static final String SUPERVISOR_AGENT_NAME = "Supervisor_agent";

    /**
     * 延迟注入的 AgentRegistry。
     * <p>
     * {@code @Lazy} 确保 Spring 注入的是一个 CGLIB 代理，
     * 真正的 AgentRegistry 实例在首次方法调用时才解析。
     * 这打破了构造期的循环依赖链：
     * <pre>
     *   AgentRegistry → SupervisorAgent → SupervisorToolProvider → AgentRegistry (延迟)
     * </pre>
     */
    private final AgentRegistry agentRegistry;

    /**
     * 缓存的工具列表（懒加载）。
     * <p>
     * 使用 volatile 保证多线程可见性。
     * 首次调用 getTools() 时通过 synchronized 块初始化，
     * 后续调用直接返回缓存，避免重复创建 AgentTool 实例。
     * <p>
     * 注意：缓存是安全的，因为 AgentRegistry 中的 Agent 列表
     * 在容器启动后不会变化（Spring Bean 生命周期保证）。
     */
    private volatile List<ToolCallback> cachedTools;

    /**
     * 构造器注入。
     * <p>
     * <b>重要：</b>这里只注入 AgentRegistry 的延迟代理，
     * <b>不</b>在构造期调用 agentRegistry 的任何方法。
     * 所有对 AgentRegistry 的访问都延迟到 {@link #getTools()} 中。
     *
     * @param agentRegistry 延迟注入的智能体注册表
     */
    public SupervisorToolProvider(@Lazy AgentRegistry agentRegistry) {
        this.agentRegistry = agentRegistry;
        log.info("SupervisorToolProvider 初始化完成（AgentRegistry 延迟注入，工具列表将在首次使用时构建）");
    }

    /**
     * 获取 Supervisor 可用的工具列表。
     * <p>
     * <b>懒加载策略：</b>
     * <ol>
     *   <li>首次调用时，从 AgentRegistry 获取所有非 Supervisor 智能体</li>
     *   <li>将每个智能体通过 {@link AgentTool#of(Agent)} 包装为 ToolCallback</li>
     *   <li>缓存结果，后续调用直接返回</li>
     * </ol>
     * <p>
     * <b>双重检查锁定（DCL）：</b>
     * 使用 volatile + synchronized 保证线程安全的懒初始化。
     * 由于 AgentRegistry 的内容在运行期不变，缓存一旦初始化就不会失效。
     *
     * @return 不可变的工具列表（排除了 Supervisor 自身）
     */
    @Override
    public List<ToolCallback> getTools() {
        // 第一次检查：避免不必要的同步
        if (cachedTools == null) {
            synchronized (this) {
                // 第二次检查：防止多线程重复初始化
                if (cachedTools == null) {
                    cachedTools = buildTools();
                }
            }
        }
        return cachedTools;
    }

    /**
     * 获取工具提供者名称。
     *
     * @return 提供者名称标识
     */
    @Override
    public String getProviderName() {
        return "SUPERVISOR_TOOLS";
    }

    // ========== 内部方法 ==========

    /**
     * 构建工具列表（仅在首次调用时执行）。
     * <p>
     * 构建流程：
     * <ol>
     *   <li>从 AgentRegistry 获取排除 Supervisor 后的智能体列表</li>
     *   <li>将每个 Agent 通过 AgentTool.of() 包装为 ToolCallback</li>
     *   <li>记录日志，便于排查工具列表问题</li>
     * </ol>
     * <p>
     * <b>注意：</b>这是第一次实际访问 AgentRegistry（触发延迟解析）。
     * 此时所有 Spring Bean 已完成初始化，循环依赖不再存在。
     *
     * @return 包装后的工具列表
     */
    private List<ToolCallback> buildTools() {
        // 此处首次触发 AgentRegistry 的实际初始化（@Lazy 代理解除）
        // 此时 SupervisorAgent 已经创建完成，循环依赖已被打破
        List<Agent<String>> agents = agentRegistry.getAgentsExcluding(SUPERVISOR_AGENT_NAME);

        List<ToolCallback> tools = agents.stream()
                .map(AgentTool::of)
                .collect(java.util.stream.Collectors.toList());

        log.info("SupervisorToolProvider 工具列表构建完成，共 {} 个子智能体工具: {}",
                tools.size(),
                tools.stream()
                        .map(tool -> tool.getToolDefinition().name())
                        .collect(java.util.stream.Collectors.joining(", ")));

        return tools;
    }
}
