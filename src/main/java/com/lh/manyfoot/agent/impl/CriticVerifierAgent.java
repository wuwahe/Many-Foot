package com.lh.manyfoot.agent.impl;

import com.lh.manyfoot.agent.context.AgentContext;
import com.lh.manyfoot.agent.core.AbstractToolAgent;
import com.lh.manyfoot.agent.domain.ArtifactBundle;
import com.lh.manyfoot.agent.domain.ReviewReport;
import com.lh.manyfoot.agent.prompt.CriticVerifierPromptProvider;
import com.lh.manyfoot.agent.strategy.SyncCallStrategy;
import com.lh.manyfoot.agent.support.SpecialistJsonUtils;
import com.lh.manyfoot.agent.tool.FullToolProvider;
import com.lh.manyfoot.models.registry.ModelResolver;
import com.lh.manyfoot.models.registry.ModelRole;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 评审与验证 Agent，负责独立事实、规则、安全和格式复核。
 */
@Component
public class CriticVerifierAgent extends AbstractToolAgent<String> {

    public CriticVerifierAgent(ModelResolver modelResolver,
                               CriticVerifierPromptProvider promptProvider,
                               FullToolProvider toolProvider) {
        super(modelResolver, promptProvider, new SyncCallStrategy(), toolProvider);
    }

    @Override
    public String getName() {
        return "Critic_Verifier_agent";
    }

    @Override
    public String getDescription() {
        return "评审与验证智能体，负责事实核验、规则安全合规检查与返工决策";
    }

    @Override
    protected ModelRole getModelRole() {
        return ModelRole.CRITIC_VERIFIER;
    }



    public ReviewReport review(String sessionId, ArtifactBundle bundle) {
        AgentContext context = AgentContext.builder()
            .sessionId(sessionId)
            .query(SpecialistJsonUtils.toJson(bundle))
            .build();
        String response = execute(context);
        return SpecialistJsonUtils.parseResponse(response, ReviewReport.class, () -> fallbackReview(response));
    }

    private ReviewReport fallbackReview(String response) {
        return ReviewReport.builder()
            .decision("REWORK")
            .score(0.0)
            .issues(List.of(ReviewReport.Issue.builder()
                .severity("HIGH")
                .message("模型响应未能解析为 ReviewReport JSON")
                .recommendation(response)
                .build()))
            .requiredFixes(List.of("重新生成可解析的 ReviewReport JSON"))
            .build();
    }

    @Override
    protected Set<String> getAvailableTools() {
        return Set.of(
                "readSandboxFile",
                "parseSandboxDocument",
                "listSandboxDirectory"
        );
    }
}
