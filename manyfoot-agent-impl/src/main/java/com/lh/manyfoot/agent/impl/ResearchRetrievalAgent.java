package com.lh.manyfoot.agent.impl;

import com.lh.manyfoot.agent.context.AgentContext;
import com.lh.manyfoot.agent.core.AbstractToolAgent;
import com.lh.manyfoot.agent.domain.EvidencePack;
import com.lh.manyfoot.agent.domain.ResearchBrief;
import com.lh.manyfoot.agent.prompt.ResearchRetrievalPromptProvider;
import com.lh.manyfoot.agent.strategy.SyncCallStrategy;
import com.lh.manyfoot.agent.support.BudgetLimitedToolCallback;
import com.lh.manyfoot.agent.support.SpecialistJsonUtils;
import com.lh.manyfoot.agent.tool.FullToolProvider;
import com.lh.manyfoot.models.registry.ModelResolver;
import com.lh.manyfoot.domain.ModelRole;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 检索与证据 Agent，负责并发检索后的证据整理。
 */
@Component
public class ResearchRetrievalAgent extends AbstractToolAgent<String> {

    public ResearchRetrievalAgent(ModelResolver modelResolver,
                                  ResearchRetrievalPromptProvider promptProvider,
                                  FullToolProvider toolProvider) {
        super(modelResolver, promptProvider, new SyncCallStrategy(), toolProvider);
    }

    @Override
    public String getName() {
        return "Research_Retrieval_agent";
    }

    @Override
    public String getDescription() {
        return "检索与证据智能体，负责搜索、去重、排序并抽取可追溯证据";
    }

    @Override
    protected ModelRole getModelRole() {
        return ModelRole.RESEARCH_RETRIEVAL;
    }

    public EvidencePack research(String sessionId, ResearchBrief brief) {
        AgentContext context = AgentContext.builder()
                .sessionId(sessionId)
                .query(SpecialistJsonUtils.toJson(brief))
                .build();
        String response = execute(context);
        return SpecialistJsonUtils.parseResponse(response, EvidencePack.class, () -> fallbackEvidence(response));
    }

    private EvidencePack fallbackEvidence(String response) {
        return EvidencePack.builder()
                .confidence(0.0)
                .informationGaps(List.of("模型响应未能解析为 EvidencePack JSON"))
                .facts(List.of(EvidencePack.Fact.builder()
                        .claim(response)
                        .confidence(0.0)
                        .build()))
                .build();
    }

    /**
     * 单次任务最多允许的搜索工具调用次数。
     */
    private static final int SEARCH_BUDGET = 3;

    @Override
    protected Set<String> getAvailableTools() {
        return Set.of(
            "webSearch",
            "deepResearch",
            "batchFetchPages",
            "webFetchPage",
            "newsSearch"
        );
    }

    /**
     * 为搜索工具添加调用预算限制，防止 ReAct 循环无节制地反复搜索。
     */
    @Override
    protected List<ToolCallback> getTools() {
        List<ToolCallback> tools = super.getTools();
        return tools.stream()
                .map(tool -> BudgetLimitedToolCallback.builder()
                        .delegate(tool)
                        .maxCalls(SEARCH_BUDGET)
                        .build())
                .map(ToolCallback.class::cast)
                .toList();
    }

    @Override
    protected Set<String> getAvailableSkills() {
        return Set.of();
    }
}
