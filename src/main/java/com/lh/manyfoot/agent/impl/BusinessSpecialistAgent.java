package com.lh.manyfoot.agent.impl;

import com.lh.manyfoot.agent.prompt.BusinessSpecialistPromptProvider;
import com.lh.manyfoot.models.registry.ModelResolver;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * 通用业务专家 Agent，负责业务规则、流程、约束和方案取舍分析。
 */
@Component
public class BusinessSpecialistAgent extends DomainSpecialistAgent {

    public BusinessSpecialistAgent(ModelResolver modelResolver,
                                   BusinessSpecialistPromptProvider promptProvider) {
        super(modelResolver, promptProvider);
    }

    @Override
    public String getName() {
        return "Business_Specialist_agent";
    }

    @Override
    public String getDescription() {
        return "通用业务专家智能体，负责业务目标、流程规则、方案取舍和风险分析";
    }

    @Override
    public SpecialistType getSpecialistType() {
        return SpecialistType.BUSINESS;
    }

    @Override
    protected String getSpecialistProfile() {
        return "聚焦业务目标、流程边界、角色职责、规则约束、收益成本、落地风险和决策建议；不替代调度或系统执行。";
    }

    @Override
    protected Collection<String> getDomainKeywords() {
        return List.of("business", "业务", "流程", "规则", "运营", "策略", "用户", "产品", "商业", "需求背景", "决策");
    }
}
