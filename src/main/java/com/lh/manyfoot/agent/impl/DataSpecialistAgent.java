package com.lh.manyfoot.agent.impl;

import com.lh.manyfoot.agent.prompt.DataSpecialistPromptProvider;
import com.lh.manyfoot.models.registry.ModelResolver;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * 数据/SQL/分析专家 Agent，负责数据口径、SQL 方案、指标分析和统计风险判断。
 */
@Component
public class DataSpecialistAgent extends DomainSpecialistAgent {

    public DataSpecialistAgent(ModelResolver modelResolver,
                               DataSpecialistPromptProvider promptProvider) {
        super(modelResolver, promptProvider);
    }

    @Override
    public String getName() {
        return "Data_Specialist_agent";
    }

    @Override
    public String getDescription() {
        return "数据/SQL/分析专家智能体，负责数据口径、查询方案、指标分析和统计风险判断";
    }

    @Override
    public SpecialistType getSpecialistType() {
        return SpecialistType.DATA;
    }

    @Override
    protected String getSpecialistProfile() {
        return "聚焦数据模型、指标口径、SQL 草稿、统计分析、数据质量、样本偏差和查询风险；只给方案和风险，不执行数据库写操作。";
    }

    @Override
    protected Collection<String> getDomainKeywords() {
        return List.of("data", "数据", "sql", "数据库", "指标", "报表", "分析", "统计", "查询", "表", "字段", "口径");
    }
}
