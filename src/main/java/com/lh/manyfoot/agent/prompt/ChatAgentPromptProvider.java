package com.lh.manyfoot.agent.prompt;

import com.lh.manyfoot.agent.context.AgentContext;
import org.springframework.stereotype.Component;

@Component
public class ChatAgentPromptProvider implements AgentPromptProvider {

    @Override
    public String buildSystemPrompt(AgentContext context) {
        return "你是一个友好的对话助手，负责处理简单的日常对话和问答。直接、简洁地回答用户问题。";
    }

    @Override
    public String buildUserInput(AgentContext context) {
        return context.getQuery();
    }
}
