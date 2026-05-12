package com.lh.manyfoot.agent.registry;

import com.lh.manyfoot.agent.core.Agent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体注册表
 * <p>
 * 通过 Spring 自动发现所有 {@code Agent<String>} 类型的 Bean，
 * 并提供按名称查找、排除、遍历等能力。
 * <p>
 * <h3>为什么需要 AgentRegistry？</h3>
 * 现有的 {@code AgentFactory} 采用硬编码方式管理智能体——每新增一个 Agent
 * 都需要修改工厂类的字段、构造器和 switch 分支。AgentRegistry 则利用
 * Spring 的泛型集合注入机制，自动收集容器中所有 {@code Agent<String>} Bean，
 * 实现"新增 Agent 只需加 @Component，无需改注册逻辑"的开闭原则。
 * <p>
 * <h3>与 AgentFactory 的关系</h3>
 * AgentRegistry <b>不替代</b> AgentFactory。AgentFactory 仍然是兼容性门面，
 * 供现有代码通过 {@code AgentType} 枚举获取智能体。AgentRegistry 提供的是
 * 动态发现能力，主要服务于 Supervisor 编排层——Supervisor 需要在运行时
 * 枚举所有可用子智能体并将其暴露为工具。
 * <p>
 * <h3>重复名称处理</h3>
 * 如果多个 Bean 返回相同的 {@code getName()} 值（理论上不应发生），
 * 注册表会记录警告日志，并保留<b>最后注册</b>的那个 Bean。
 * 返回的列表始终保持注册顺序（LinkedHashMap 插入序），确保
 * Prompt 拼接和工具列表的确定性。
 * <p>
 * <h3>循环依赖注意事项</h3>
 * 当 Supervisor 自身也是一个 {@code Agent<String>} Bean 时，
 * 本注册表会自动收集到它。如果 SupervisorToolProvider 在构造期
 * 就急切地遍历注册表内容，会触发：
 * <pre>
 *   AgentRegistry → SupervisorAgent → SupervisorToolProvider → AgentRegistry
 * </pre>
 * 因此，消费方（如 SupervisorToolProvider）必须使用 {@code @Lazy}
 * 注入 AgentRegistry，或在使用时排除自身（{@code getAgentsExcluding("Supervisor_agent")}）。
 * 注册表本身不负责排除任何特定 Agent——排除逻辑由消费方按需决定。
 *
 * @author airx
 * @see Agent
 * @see com.lh.manyfoot.agent.factory.AgentFactory
 */
@Slf4j
@Component
public class AgentRegistry {

    /**
     * 按名称索引的智能体映射。
     * 使用 LinkedHashMap 保持插入顺序，确保 getAllAgents() 返回确定性列表。
     */
    private final Map<String, Agent<String>> agentMap;

    /**
     * 构造器注入：Spring 会自动收集容器中所有 {@code Agent<String>} 类型的 Bean。
     * <p>
     * 注意：这里使用的是 Spring 的泛型集合注入机制。
     * 抽象类（如 DomainSpecialistAgent）不会被收集，只有具体的 @Component 子类才会。
     * 收集范围包括所有具体的 @Component 子类（如 PlannerRouter、ResearchRetrieval、
     * 各 DomainSpecialist、ToolActionExecutor、Code、Chat、Supervisor 等）。
     * 抽象父类（如 DomainSpecialistAgent）不会被收集。
     * Supervisor 自身也会被收集；消费方如需排除，应使用
     * {@code getAgentsExcluding("Supervisor_agent")} 或 {@code @Lazy} 注入。
     *
     * @param agents Spring 自动注入的所有 Agent<String> Bean
     */
    public AgentRegistry(List<Agent<String>> agents) {
        this.agentMap = new LinkedHashMap<>();
        for (Agent<String> agent : agents) {
            String name = agent.getName();
            if (name == null || name.isBlank()) {
                log.warn("发现名称为空的 Agent Bean: {}，已跳过注册", agent.getClass().getSimpleName());
                continue;
            }
            Agent<String> previous = agentMap.put(name, agent);
            if (previous != null) {
                // 理论上不应发生——每个 Agent 应有唯一名称
                log.warn("检测到重复的 Agent 名称 '{}'：{} 被 {} 覆盖",
                        name,
                        previous.getClass().getSimpleName(),
                        agent.getClass().getSimpleName());
            }
        }
        log.info("AgentRegistry 初始化完成，共注册 {} 个智能体: {}", agentMap.size(), agentMap.keySet());
    }

    /**
     * 根据智能体名称获取实例。
     *
     * @param agentName 智能体名称（即 {@code Agent.getName()} 的返回值）
     * @return 对应的智能体，不存在时返回 null
     */
    public Agent<String> getAgent(String agentName) {
        return agentMap.get(agentName);
    }

    /**
     * 获取所有已注册智能体的<b>不可变</b>列表。
     * <p>
     * 列表顺序与注册顺序一致（即 Spring Bean 创建顺序），
     * 可用于 Prompt 拼接、工具列表构建等需要确定性顺序的场景。
     *
     * @return 不可变的智能体列表（防御性拷贝）
     */
    public List<Agent<String>> getAllAgents() {
        return Collections.unmodifiableList(new ArrayList<>(agentMap.values()));
    }

    /**
     * 获取排除指定名称后的智能体列表。
     * <p>
     * 典型用途：SupervisorToolProvider 在构建工具列表时需要排除自身，
     * 避免 Supervisor 调用自己的循环问题。
     * <p>
     * 返回的列表保持原始注册顺序。
     *
     * @param excludedNames 需要排除的智能体名称（可变参数）
     * @return 排除后的不可变智能体列表
     */
    public List<Agent<String>> getAgentsExcluding(String... excludedNames) {
        if (excludedNames == null || excludedNames.length == 0) {
            return getAllAgents();
        }
        // 构建排除集合，便于 O(1) 查找
        java.util.Set<String> excludeSet = new java.util.HashSet<>(java.util.Arrays.asList(excludedNames));
        List<Agent<String>> result = new ArrayList<>();
        for (Map.Entry<String, Agent<String>> entry : agentMap.entrySet()) {
            if (!excludeSet.contains(entry.getKey())) {
                result.add(entry.getValue());
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * 判断指定名称的智能体是否已注册。
     *
     * @param agentName 智能体名称
     * @return 如果已注册返回 true
     */
    public boolean containsAgent(String agentName) {
        return agentMap.containsKey(agentName);
    }

    /**
     * 获取已注册智能体的数量。
     *
     * @return 智能体数量
     */
    public int size() {
        return agentMap.size();
    }
}
