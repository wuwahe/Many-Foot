package com.lh.manyfoot.agent.prompt;

import cn.hutool.core.util.StrUtil;
import com.lh.manyfoot.agent.context.AgentContext;
import com.lh.manyfoot.orchestrator.prompts.ManusPrompts;
import org.springframework.stereotype.Component;

/**
 * 执行器智能体提示词提供者
 *
 * @author airx
 */
@Component
public class ExecutorPromptProvider implements AgentPromptProvider {

    @Override
    public String buildSystemPrompt(AgentContext context) {
        return ManusPrompts.buildExecutorPrompt(
            context.getQuery(),
            context.getObservations(),
            context.getSessionId()
        );
    }

    @Override
    public String buildUserInput(AgentContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("任务清单：").append(context.getQuery()).append("\n");
        if (StrUtil.isNotBlank(context.getObservations())) {
            sb.append("历史观察结果：").append(context.getObservations());
        }
        return sb.toString();
    }
}
