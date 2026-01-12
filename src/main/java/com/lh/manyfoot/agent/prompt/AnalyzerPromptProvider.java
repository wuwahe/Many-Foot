package com.lh.manyfoot.agent.prompt;

import com.lh.manyfoot.agent.context.AgentContext;
import com.lh.manyfoot.orchestrator.prompts.ManusPrompts;
import org.springframework.stereotype.Component;

/**
 * 分析智能体提示词提供者
 *
 * @author airx
 */
@Component
public class AnalyzerPromptProvider implements AgentPromptProvider {

    @Override
    public String buildSystemPrompt(AgentContext context) {
        return ManusPrompts.buildAnalyzerAgentPrompt(context.getSessionId());
    }

    @Override
    public String buildUserInput(AgentContext context) {
        return "请分析以下任务并制定执行计划：\n" + context.getQuery();
    }
}
