package com.lh.manyfoot.agent.core;

import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.lh.manyfoot.agent.prompt.AgentPromptProvider;
import com.lh.manyfoot.agent.strategy.ExecutionStrategy;
import com.lh.manyfoot.agent.support.FilteringSkillRegistry;
import com.lh.manyfoot.agent.tool.AgentToolProvider;
import com.lh.manyfoot.models.registry.ModelResolver;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 支持工具调用的抽象智能体基类
 * 通过AgentToolProvider获取工具列表
 *
 * @param <R> 执行结果类型
 * @author airx
 */
public abstract class AbstractToolAgent<R> extends AbstractAgent<R> {

    protected final AgentToolProvider toolProvider;

    @Autowired(required = false)
    private SkillRegistry skillRegistry;

    protected AbstractToolAgent(ModelResolver modelResolver,
                                AgentPromptProvider promptProvider,
                                ExecutionStrategy<R> executionStrategy,
                                AgentToolProvider toolProvider) {
        super(modelResolver, promptProvider, executionStrategy);
        this.toolProvider = toolProvider;
    }

    protected abstract Set<String> getAvailableTools();

    /**
     * 返回该智能体可用的 skill 名称集合。
     * <p>
     * 子类可以覆盖此方法以声明自己需要的 skills。
     * 默认返回空集合，表示不加载任何 skills。
     *
     * @return skill 名称集合
     */
    protected Set<String> getAvailableSkills() {
        return Collections.emptySet();
    }

    @Override
    protected List<ToolCallback> getTools() {
        Set<String> availableTools = getAvailableTools();
        return toolProvider.getTools().stream()
            .filter(tool -> availableTools.contains(tool.getToolDefinition().name()))
            .toList();
    }

    SkillsAgentHook getSkillsAgentHook() {
        Set<String> availableSkills = getAvailableSkills();
        if (availableSkills.isEmpty() || skillRegistry == null) {
            return null;
        }
        SkillRegistry filteredRegistry = new FilteringSkillRegistry(skillRegistry, availableSkills);
        return SkillsAgentHook.builder()
            .skillRegistry(filteredRegistry)
            .autoReload(true)
            .build();
    }
}
