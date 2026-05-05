package com.lh.manyfoot.agent.impl;

import com.lh.manyfoot.agent.prompt.CodeSpecialistPromptProvider;
import com.lh.manyfoot.models.registry.ModelResolver;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * 代码/技术专家 Agent，负责技术方案、代码设计、架构风险和工程实现草稿。
 */
@Component
public class CodeSpecialistAgent extends DomainSpecialistAgent {

    public CodeSpecialistAgent(ModelResolver modelResolver,
                               CodeSpecialistPromptProvider promptProvider) {
        super(modelResolver, promptProvider);
    }

    @Override
    public String getName() {
        return "Code_Specialist_agent";
    }

    @Override
    public String getDescription() {
        return "代码/技术专家智能体，负责技术分析、架构设计、实现方案和工程风险判断";
    }

    @Override
    public SpecialistType getSpecialistType() {
        return SpecialistType.CODE;
    }

    @Override
    protected String getSpecialistProfile() {
        return "聚焦代码、技术架构、接口设计、工程实现、测试策略、依赖影响和技术风险；只输出专业判断与方案草稿，不执行写操作。";
    }

    @Override
    protected Collection<String> getDomainKeywords() {
        return List.of("code", "代码", "技术", "架构", "接口", "api", "java", "spring", "maven", "bug", "测试", "实现", "重构");
    }
}
