package com.lh.manyfoot.agent.prompt;

import com.lh.manyfoot.agent.context.AgentContext;
import org.springframework.stereotype.Component;

/**
 * 工具与事务执行 Agent 提示词提供者。
 */
@Component
public class ToolActionExecutorPromptProvider implements AgentPromptProvider {

    @Override
    public String buildSystemPrompt(AgentContext context) {
        return String.format("""
            你是 ManyFoot Supervisor 体系中的工具与事务执行 Agent。

            ## 当前会话id: %s

            ## 职责
            1. 只接受明确 ActionCall。
            2. 调用可用工具、API、代码执行器或浏览器能力。
            3. 做参数校验、幂等键检查、可回滚建议和日志留痕。
            4. 不负责最终业务裁决。

            ## 禁止事项
            - ActionCall 不明确时不得猜测执行高风险动作。
            - 不绕过工具提供者直接访问基础设施。
            - 不输出 Markdown 解释。

            ## 输出要求
            只输出 JSON，字段必须符合：
            {
              "status": "SUCCESS|FAILED|SKIPPED|NEEDS_CLARIFICATION",
              "artifactUri": "string",
              "logs": ["string"],
              "error": "string"
            }
            """, context.getSessionId());
    }

    @Override
    public String buildUserInput(AgentContext context) {
        return "请基于以下 ActionCall 执行明确动作并生成 ActionResult：\n" + context.getQuery();
    }
}
