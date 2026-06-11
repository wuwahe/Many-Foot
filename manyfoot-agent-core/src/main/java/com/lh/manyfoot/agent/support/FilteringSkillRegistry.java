package com.lh.manyfoot.agent.support;

import com.alibaba.cloud.ai.graph.skills.SkillMetadata;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 带过滤功能的 SkillRegistry 代理类。
 * <p>
 * 包装一个 SkillRegistry，只允许访问指定的 skill 名称集合。
 * 用于实现按智能体隔离 skills 加载：每个智能体只看到自己声明的 skills。
 *
 * @author airx
 * @see com.lh.manyfoot.agent.core.AbstractToolAgent#getAvailableSkills()
 */
public class FilteringSkillRegistry implements SkillRegistry {

    private final SkillRegistry delegate;
    private final Set<String> allowedSkillNames;

    /**
     * 创建过滤代理。
     *
     * @param delegate          原始 SkillRegistry
     * @param allowedSkillNames 允许访问的 skill 名称集合（null 或空集合表示不允许任何 skill）
     */
    public FilteringSkillRegistry(SkillRegistry delegate, Set<String> allowedSkillNames) {
        this.delegate = delegate;
        this.allowedSkillNames = allowedSkillNames != null ? allowedSkillNames : Set.of();
    }

    @Override
    public Optional<SkillMetadata> get(String name) {
        if (name == null || !allowedSkillNames.contains(name)) {
            return Optional.empty();
        }
        return delegate.get(name);
    }

    @Override
    public List<SkillMetadata> listAll() {
        return delegate.listAll().stream()
                .filter(skill -> allowedSkillNames.contains(skill.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean contains(String name) {
        return allowedSkillNames.contains(name) && delegate.contains(name);
    }

    @Override
    public int size() {
        return (int) delegate.listAll().stream()
                .filter(skill -> allowedSkillNames.contains(skill.getName()))
                .count();
    }

    @Override
    public void reload() {
        delegate.reload();
    }

    @Override
    public String readSkillContent(String name) throws IOException {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Skill name must not be null or empty");
        }
        if (!allowedSkillNames.contains(name)) {
            throw new IllegalStateException("Skill not found or not allowed: " + name);
        }
        return delegate.readSkillContent(name);
    }

    @Override
    public String getSkillLoadInstructions() {
        return delegate.getSkillLoadInstructions();
    }

    @Override
    public String getRegistryType() {
        return delegate.getRegistryType() + "-filtered";
    }

    @Override
    public SystemPromptTemplate getSystemPromptTemplate() {
        return delegate.getSystemPromptTemplate();
    }

    /**
     * 获取当前允许访问的 skill 名称集合。
     *
     * @return 允许访问的 skill 名称集合
     */
    public Set<String> getAllowedSkillNames() {
        return allowedSkillNames;
    }
}
