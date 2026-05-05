package com.lh.manyfoot.agent.impl;

import com.lh.manyfoot.agent.context.AgentContext;
import com.lh.manyfoot.agent.core.AbstractAgent;
import com.lh.manyfoot.agent.prompt.ChatAgentPromptProvider;
import com.lh.manyfoot.agent.strategy.SyncCallStrategy;
import com.lh.manyfoot.models.registry.ModelResolver;
import com.lh.manyfoot.models.registry.ModelRole;
import org.springframework.stereotype.Component;

@Component
public class ChatAgent extends AbstractAgent<String> {

    public ChatAgent(ModelResolver modelResolver, ChatAgentPromptProvider promptProvider) {
        super(modelResolver, promptProvider, new SyncCallStrategy());
    }

    @Override
    public String getName() {
        return "Chat_agent";
    }

    @Override
    public String getDescription() {
        return "普通对话智能体，处理简单的日常对话和问答";
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
}
