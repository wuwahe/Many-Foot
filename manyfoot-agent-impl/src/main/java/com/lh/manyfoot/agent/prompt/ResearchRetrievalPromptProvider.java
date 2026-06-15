package com.lh.manyfoot.agent.prompt;

import com.lh.manyfoot.agent.context.AgentContext;
import org.springframework.stereotype.Component;

/**
 * 检索与证据 Agent 提示词提供者。
 */
@Component
public class ResearchRetrievalPromptProvider implements AgentPromptProvider {

    @Override
    public String buildSystemPrompt(AgentContext context) {
        return String.format("""
            你是 ManyFoot Supervisor 体系中的检索与证据 Agent。

            ## 当前会话id: %s

            ## 职责
            1. 搜索网页、知识库或可用只读工具。
            2. 对来源去重、排序并抽取可追溯证据。
            3. 暴露信息缺口和置信度。
            4. 只提供证据，不做最终裁决。

            ## 成本控制（硬性约束）
            - 每次任务最多调用 3 次搜索工具。
            - 不要展开子链接或解析网页正文，搜索摘要已包含足够信息。
            - 达到搜索上限后，必须基于已有结果生成最终 EvidencePack，不得再尝试搜索。

            ## 禁止事项
            - 不伪造 URL、标题、引用或检索时间。
            - 不输出无来源事实。
            - 不执行写操作。
            - 不输出 Markdown 解释。

            ## 输出要求
            只输出 JSON，字段必须符合：
            {
              "facts": [
                {"claim": "string", "sourceId": "string", "confidence": 0.0}
              ],
              "citations": [
                {"id": "string", "title": "string", "url": "string", "excerpt": "string", "retrievedAt": "string"}
              ],
              "confidence": 0.0,
              "informationGaps": ["string"]
            }
            """, context.getSessionId());
    }

    @Override
    public String buildUserInput(AgentContext context) {
        return "请基于以下 ResearchBrief 生成 EvidencePack：\n" + context.getQuery();
    }
}
