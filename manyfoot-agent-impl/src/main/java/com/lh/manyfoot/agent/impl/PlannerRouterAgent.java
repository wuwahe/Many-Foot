package com.lh.manyfoot.agent.impl;

import com.lh.manyfoot.agent.context.AgentContext;
import com.lh.manyfoot.agent.core.AbstractAgent;
import com.lh.manyfoot.agent.domain.PlanGraph;
import com.lh.manyfoot.agent.domain.TaskSpec;
import com.lh.manyfoot.agent.prompt.PlannerRouterPromptProvider;
import com.lh.manyfoot.agent.strategy.SyncCallStrategy;
import com.lh.manyfoot.agent.support.SpecialistJsonUtils;
import com.lh.manyfoot.models.registry.ModelResolver;
import com.lh.manyfoot.domain.ModelRole;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 规划与路由 Agent，负责生成 Supervisor 可执行的计划图。
 */
@Component
public class PlannerRouterAgent extends AbstractAgent<String> {

    public PlannerRouterAgent(ModelResolver modelResolver,
                              PlannerRouterPromptProvider promptProvider) {
        super(modelResolver, promptProvider, new SyncCallStrategy());
    }

    @Override
    public String getName() {
        return "Planner_Router_agent";
    }

    @Override
    public String getDescription() {
        return "规划与路由智能体，负责拆解目标、选择协作模式并定义完成标准";
    }

    @Override
    protected ModelRole getModelRole() {
        return ModelRole.PLANNER_ROUTER;
    }

    public PlanGraph plan(String sessionId, TaskSpec taskSpec) {
        AgentContext context = AgentContext.builder()
            .sessionId(sessionId)
            .query(SpecialistJsonUtils.toJson(taskSpec))
            .build();
        String response = execute(context);
        return SpecialistJsonUtils.parseResponse(response, PlanGraph.class, () -> fallbackPlan(taskSpec, response));
    }

    private PlanGraph fallbackPlan(TaskSpec taskSpec, String response) {
        return PlanGraph.builder()
            .mode("SEQUENTIAL")
            .completionCriteria(List.of("完成用户目标: " + taskSpec.getGoal()))
            .budget("未从模型响应中解析到预算，使用默认单轮计划")
            .replanningPolicy("当任一步骤失败时回到 Planner_Router_agent 重新规划")
            .steps(List.of(PlanGraph.PlanStep.builder()
                .id("step-1")
                .name("处理任务")
                .description(response)
                .targetAgent("DOMAIN_SPECIALIST_AGENT")
                .acceptanceCriteria(List.of("输出可审查的领域草稿"))
                .build()))
            .build();
    }
}
