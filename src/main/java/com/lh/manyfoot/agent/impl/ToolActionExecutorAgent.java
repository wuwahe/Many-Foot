package com.lh.manyfoot.agent.impl;

import com.lh.manyfoot.agent.context.AgentContext;
import com.lh.manyfoot.agent.core.AbstractToolAgent;
import com.lh.manyfoot.agent.domain.ActionCall;
import com.lh.manyfoot.agent.domain.ActionResult;
import com.lh.manyfoot.agent.prompt.ToolActionExecutorPromptProvider;
import com.lh.manyfoot.agent.strategy.SyncCallStrategy;
import com.lh.manyfoot.agent.support.SpecialistJsonUtils;
import com.lh.manyfoot.agent.tool.FullToolProvider;
import com.lh.manyfoot.models.registry.ModelResolver;
import com.lh.manyfoot.models.registry.ModelRole;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 工具与事务执行 Agent，负责明确动作的真实执行。
 */
@Component
public class ToolActionExecutorAgent extends AbstractToolAgent<String> {

    public ToolActionExecutorAgent(ModelResolver modelResolver,
                                   ToolActionExecutorPromptProvider promptProvider,
                                   FullToolProvider toolProvider) {
        super(modelResolver, promptProvider, new SyncCallStrategy(), toolProvider);
    }

    @Override
    public String getName() {
        return "Tool_Action_Executor_agent";
    }

    @Override
    public String getDescription() {
        return "工具与事务执行智能体，负责明确动作调用、参数校验、幂等与日志留痕";
    }

    @Override
    protected ModelRole getModelRole() {
        return ModelRole.TOOL_ACTION_EXECUTOR;
    }

    public ActionResult executeAction(String sessionId, ActionCall actionCall) {
        AgentContext context = AgentContext.builder()
            .sessionId(sessionId)
            .query(SpecialistJsonUtils.toJson(actionCall))
            .build();
        String response = execute(context);
        return SpecialistJsonUtils.parseResponse(response, ActionResult.class, () -> fallbackResult(response));
    }

    private ActionResult fallbackResult(String response) {
        return ActionResult.builder()
            .status("FAILED")
            .logs(List.of(response))
            .error("模型响应未能解析为 ActionResult JSON")
            .build();
    }

    @Override
    protected Set<String> getAvailableTools() {
        return Set.of(
                "writeSandboxFile",
                "readSandboxFile",
                "parseSandboxDocument",
                "listSandboxDirectory",
                "executeShell",
                "executePython",
                "installPythonPackage"
        );
    }

}
