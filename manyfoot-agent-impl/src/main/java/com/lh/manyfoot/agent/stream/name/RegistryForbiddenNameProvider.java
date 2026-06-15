package com.lh.manyfoot.agent.stream.name;

import com.lh.manyfoot.agent.registry.AgentRegistry;
import com.lh.manyfoot.agent.tool.FullToolProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 基于注册表的禁词提供者
 * <p>
 * 从 {@link AgentRegistry} 和 {@link FullToolProvider} 中动态收集所有内部标识符，
 * 并补充框架级硬编码术语，构建完整的禁词集合。
 * <p>
 * <h3>循环依赖防护</h3>
 * 与 {@code SupervisorToolProvider} 同理，本类使用 {@code @Lazy} 注入
 * AgentRegistry 和 FullToolProvider，避免构造期触发循环依赖：
 * <pre>
 *   AgentRegistry → SupervisorAgent → ... → RegistryForbiddenNameProvider → AgentRegistry (延迟)
 * </pre>
 * 真正的实例解析延迟到 {@link #forbiddenTokens()} 首次调用时。
 * <p>
 * <h3>缓存策略</h3>
 * 使用双重检查锁定（DCL）保证线程安全的懒初始化。
 * 缓存一旦构建就不会失效，因为 AgentRegistry 和 FullToolProvider 的内容
 * 在 Spring 容器启动后保持不变。
 *
 * @author airx
 * @see ForbiddenNameProvider
 * @see AgentRegistry
 * @see FullToolProvider
 */
@Slf4j
@Component
public class RegistryForbiddenNameProvider implements ForbiddenNameProvider {

    /**
     * 硬编码的框架级禁词。
     * <p>
     * 这些术语不会出现在 Agent 名称或工具名称中，
     * 但属于内部实现细节，不应暴露给终端用户。
     */
    private static final List<String> HARDCODED_FORBIDDEN = List.of(
            "sub-agent",
            "sub_agent",
            "\u5b50\u667a\u80fd\u4f53",       // 子智能体
            "AgentTool",
            "ToolCallback",
            "tool_call",
            "ReactAgent"
    );

    /**
     * 延迟注入的智能体注册表。
     * <p>
     * {@code @Lazy} 确保 Spring 注入的是 CGLIB 代理，
     * 真正的实例在首次方法调用时才解析。
     */
    private final AgentRegistry agentRegistry;

    /**
     * 延迟注入的完整工具提供者。
     */
    private final FullToolProvider fullToolProvider;

    /**
     * 缓存的禁词集合（DCL 懒加载）。
     * <p>
     * 使用 volatile 保证多线程可见性。
     * 内容经过 Pattern.quote() 处理，可直接用于正则匹配。
     */
    private volatile Set<String> cache;

    /**
     * 构造器注入。
     * <p>
     * <b>重要：</b>只注入延迟代理，不在构造期调用任何 registry 方法。
     *
     * @param agentRegistry   延迟注入的智能体注册表
     * @param fullToolProvider 延迟注入的完整工具提供者
     */
    public RegistryForbiddenNameProvider(
            @Lazy AgentRegistry agentRegistry,
            @Lazy FullToolProvider fullToolProvider) {
        this.agentRegistry = agentRegistry;
        this.fullToolProvider = fullToolProvider;
        log.info("RegistryForbiddenNameProvider 初始化完成（禁词集合将在首次使用时构建）");
    }

    /**
     * 获取所有禁词 token 集合。
     * <p>
     * <b>懒加载策略：</b>
     * <ol>
     *   <li>首次调用时，从 AgentRegistry 收集所有 Agent 名称</li>
     *   <li>从 FullToolProvider 收集所有工具定义名称</li>
     *   <li>合并硬编码禁词</li>
     *   <li>按长度降序排序（保证最长匹配优先）</li>
     *   <li>对每个 token 执行 Pattern.quote() 使其可用于正则</li>
     *   <li>缓存结果，后续调用直接返回</li>
     * </ol>
     *
     * @return 不可变的禁词集合（元素已 Pattern.quote() 处理）
     */
    @Override
    public Set<String> forbiddenTokens() {
        // 第一次检查：避免不必要的同步
        if (cache == null) {
            synchronized (this) {
                // 第二次检查：防止多线程重复初始化
                if (cache == null) {
                    cache = buildForbiddenTokens();
                }
            }
        }
        return cache;
    }

    /**
     * 构建禁词集合（仅在首次调用时执行）。
     * <p>
     * 收集来源：
     * <ol>
     *   <li>AgentRegistry 中所有 Agent 的 getName()</li>
     *   <li>FullToolProvider 中所有 ToolCallback 的 toolDefinition.name()</li>
     *   <li>硬编码框架术语</li>
     * </ol>
     *
     * @return 不可变的已排序、已转义禁词集合
     */
    private Set<String> buildForbiddenTokens() {
        // 使用 LinkedHashSet 保持插入顺序（去重后按长度排序）
        Set<String> rawTokens = new LinkedHashSet<>();

        // 1. 收集所有 Agent 名称
        List<com.lh.manyfoot.agent.core.Agent<String>> agents = agentRegistry.getAllAgents();
        for (com.lh.manyfoot.agent.core.Agent<String> agent : agents) {
            String name = agent.getName();
            if (name != null && !name.isBlank()) {
                rawTokens.add(name);
            }
        }
        log.debug("从 AgentRegistry 收集到 {} 个 Agent 名称", agents.size());

        // 2. 收集所有工具定义名称
        List<ToolCallback> tools = fullToolProvider.getTools();
        for (ToolCallback tool : tools) {
            String toolName = tool.getToolDefinition().name();
            if (toolName != null && !toolName.isBlank()) {
                rawTokens.add(toolName);
            }
        }
        log.debug("从 FullToolProvider 收集到 {} 个工具名称", tools.size());

        // 3. 合并硬编码禁词
        rawTokens.addAll(HARDCODED_FORBIDDEN);

        // 4. 按长度降序排序（保证最长匹配优先）
        List<String> sorted = rawTokens.stream()
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .toList();

        // 5. 对每个 token 执行 Pattern.quote()，使其成为正则字面量
        Set<String> quoted = new LinkedHashSet<>();
        for (String token : sorted) {
            quoted.add(Pattern.quote(token));
        }

        Set<String> result = Collections.unmodifiableSet(quoted);
        log.info("禁词集合构建完成，共 {} 个原始 token（排序+转义后 {} 个）: {}",
                rawTokens.size(), result.size(), rawTokens);
        return result;
    }
}
