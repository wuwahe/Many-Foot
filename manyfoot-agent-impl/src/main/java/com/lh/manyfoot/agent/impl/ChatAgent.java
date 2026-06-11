package com.lh.manyfoot.agent.impl;

import com.lh.manyfoot.agent.context.AgentAttachment;
import com.lh.manyfoot.agent.context.AgentContext;
import com.lh.manyfoot.agent.core.AbstractAgent;
import com.lh.manyfoot.agent.core.AbstractToolAgent;
import com.lh.manyfoot.agent.core.StreamingAgent;
import com.lh.manyfoot.agent.prompt.ChatAgentPromptProvider;
import com.lh.manyfoot.agent.prompt.ToolActionExecutorPromptProvider;
import com.lh.manyfoot.agent.strategy.SyncCallStrategy;
import com.lh.manyfoot.agent.tool.FullToolProvider;
import com.lh.manyfoot.models.registry.ModelResolver;
import com.lh.manyfoot.domain.ModelRole;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class ChatAgent extends AbstractToolAgent<String> {

    public ChatAgent(ModelResolver modelResolver,
                                   ToolActionExecutorPromptProvider promptProvider,
                                   FullToolProvider toolProvider) {
        super(modelResolver, promptProvider, new SyncCallStrategy(), toolProvider);
    }

    @Override
    public String getName() {
        return "Chat_agent";
    }

    @Override
    public String getDescription() {
        return "多模态文档分析助手，支持图片理解、文档提取、图表分析和日常对话";
    }

    @Override
    protected ModelRole getModelRole() {
        return ModelRole.CHAT;
    }

    public String chat(String sessionId, String message) {
        AgentContext context = AgentContext.builder()
            .sessionId(sessionId)
            .query(message)
            .build();
        return execute(context);
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
