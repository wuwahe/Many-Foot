package com.lh.manyfoot.agent.prompt;

import cn.hutool.core.util.StrUtil;
import com.lh.manyfoot.agent.context.AgentContext;
import com.lh.manyfoot.orchestrator.prompts.ManusPrompts;
import org.springframework.stereotype.Component;

/**
 * 观察者智能体提示词提供者
 *
 * @author airx
 */
@Component
public class ObserverPromptProvider implements AgentPromptProvider {

    @Override
    public String buildSystemPrompt(AgentContext context) {
        return ManusPrompts.buildObserverAgentPrompt(context.getSessionId());
    }

    @Override
    public String buildUserInput(AgentContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 原始任务\n").append(context.getQuery()).append("\n\n");
        sb.append("## 执行器输出结果\n").append(context.getExecuteResult()).append("\n\n");

        if (StrUtil.isNotBlank(context.getObservations())) {
            sb.append("## 历史观察记录\n").append(context.getObservations()).append("\n\n");
        }

        sb.append("请对以上执行结果进行观察和验证。");
        return sb.toString();
    }
}
