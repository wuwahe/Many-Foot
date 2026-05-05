package com.lh.manyfoot.agent.factory;

import com.lh.manyfoot.agent.core.Agent;
import com.lh.manyfoot.agent.impl.BusinessSpecialistAgent;
import com.lh.manyfoot.agent.impl.ChatAgent;
import com.lh.manyfoot.agent.impl.CodeSpecialistAgent;
import com.lh.manyfoot.agent.impl.CriticVerifierAgent;
import com.lh.manyfoot.agent.impl.DataSpecialistAgent;
import com.lh.manyfoot.agent.impl.DocumentSpecialistAgent;
import com.lh.manyfoot.agent.impl.PlannerRouterAgent;
import com.lh.manyfoot.agent.impl.ResearchRetrievalAgent;
import com.lh.manyfoot.agent.impl.SupervisorAgent;
import com.lh.manyfoot.agent.impl.ToolActionExecutorAgent;
import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * 智能体工厂
 * 提供统一的智能体获取入口
 *
 * @author airx
 */
@Component
@Getter
public class AgentFactory {

    private final PlannerRouterAgent plannerRouterAgent;
    private final ResearchRetrievalAgent researchRetrievalAgent;
    private final CodeSpecialistAgent codeSpecialistAgent;
    private final BusinessSpecialistAgent businessSpecialistAgent;
    private final DocumentSpecialistAgent documentSpecialistAgent;
    private final DataSpecialistAgent dataSpecialistAgent;
    private final ToolActionExecutorAgent toolActionExecutorAgent;
    private final CriticVerifierAgent criticVerifierAgent;
    private final ChatAgent chatAgent;
    // Supervisor 编排智能体 —— AgentFactory 仅作为兼容入口持有引用，
    // Supervisor 的实际编排逻辑由 SupervisorAgent 自身通过 ReactAgent tool_call 驱动，
    // 不依赖 AgentFactory。
    private final SupervisorAgent supervisorAgent;

    public AgentFactory(PlannerRouterAgent plannerRouterAgent,
                        ResearchRetrievalAgent researchRetrievalAgent,
                        CodeSpecialistAgent codeSpecialistAgent,
                        BusinessSpecialistAgent businessSpecialistAgent,
                        DocumentSpecialistAgent documentSpecialistAgent,
                        DataSpecialistAgent dataSpecialistAgent,
                        ToolActionExecutorAgent toolActionExecutorAgent,
                        CriticVerifierAgent criticVerifierAgent,
                        ChatAgent chatAgent,
                        SupervisorAgent supervisorAgent) {
        this.plannerRouterAgent = plannerRouterAgent;
        this.researchRetrievalAgent = researchRetrievalAgent;
        this.codeSpecialistAgent = codeSpecialistAgent;
        this.businessSpecialistAgent = businessSpecialistAgent;
        this.documentSpecialistAgent = documentSpecialistAgent;
        this.dataSpecialistAgent = dataSpecialistAgent;
        this.toolActionExecutorAgent = toolActionExecutorAgent;
        this.criticVerifierAgent = criticVerifierAgent;
        this.chatAgent = chatAgent;
        this.supervisorAgent = supervisorAgent;
    }

    /**
     * 根据类型获取智能体
     *
     * @param type 智能体类型
     * @return 智能体实例
     */
    public Agent<?> getAgent(AgentType type) {
        return switch (type) {
            case PLANNER_ROUTER -> plannerRouterAgent;
            case RESEARCH_RETRIEVAL -> researchRetrievalAgent;
            // DOMAIN_SPECIALIST 是历史兼容入口；执行期的 DOMAIN_SPECIALIST_AGENT 会由 StepDispatcher
            // 按任务内容动态选择具体专家。直接通过工厂获取时返回覆盖面最宽的通用业务专家，避免重新引入第五个“泛领域专家”实例。
            case DOMAIN_SPECIALIST, BUSINESS_SPECIALIST -> businessSpecialistAgent;
            case CODE_SPECIALIST -> codeSpecialistAgent;
            case DOCUMENT_SPECIALIST -> documentSpecialistAgent;
            case DATA_SPECIALIST -> dataSpecialistAgent;
            case TOOL_ACTION_EXECUTOR -> toolActionExecutorAgent;
            case CRITIC_VERIFIER -> criticVerifierAgent;
            case CHAT -> chatAgent;
            case SUPERVISOR -> supervisorAgent;
        };
    }

    public enum AgentType {
        PLANNER_ROUTER,
        RESEARCH_RETRIEVAL,
        DOMAIN_SPECIALIST,
        CODE_SPECIALIST,
        BUSINESS_SPECIALIST,
        DOCUMENT_SPECIALIST,
        DATA_SPECIALIST,
        TOOL_ACTION_EXECUTOR,
        CRITIC_VERIFIER,
        CHAT,
        SUPERVISOR
    }
}
