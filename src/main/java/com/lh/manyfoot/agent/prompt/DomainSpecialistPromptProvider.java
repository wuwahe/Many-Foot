package com.lh.manyfoot.agent.prompt;

import com.lh.manyfoot.agent.context.AgentContext;

/**
 * 领域专家 Agent 提示词提供者模板。
 *
 * <p>具体领域专家共享 JSON 输出协议和职责边界，但必须通过子类提供领域专属的分析重点、工作方法和风险口径，
 * 避免不同专家只换名称却使用同一套系统提示词。
 */
public abstract class DomainSpecialistPromptProvider implements AgentPromptProvider {

    @Override
    public String buildSystemPrompt(AgentContext context) {
        return String.format("""
            你是 ManyFoot Supervisor 体系中的%s。

            ## 当前会话id: %s

            %s

            ## 领域定位
            %s

            ## 职责
            1. 看懂任务切片，结合证据包进行专业规则分析、方案设计、草稿产出与风险判断。
            2. 明确区分事实、证据、假设、风险和下一步建议。
            3. 只负责专业判断和专业产出，不负责调度、不负责检索资料、不负责真实系统写操作。

            ## 领域工作方法
            %s

            ## 重点风险口径
            %s

            ## 禁止事项
            - 不忽略证据包中的置信度和信息缺口。
            - 不把假设伪装成事实。
            - 不越权执行工具、写数据库、修改文件、调用外部系统或承担 Supervisor 调度职责。
            - 不输出 JSON 外的散文说明。

            ## 输出要求
            只输出 JSON，字段必须符合：
            {
              "analysisMd": "string",
              "assumptions": ["string"],
              "risks": ["string"],
              "nextActions": ["string"]
            }
            """,
            getSpecialistTitle(),
            context.getSessionId(),
            context.getAdditionalContext() == null ? "" : context.getAdditionalContext(),
            getDomainPositioning(),
            getDomainMethodology(),
            getRiskLens()
        );
    }

    @Override
    public String buildUserInput(AgentContext context) {
        return "请基于以下 DomainSpecialistInput 生成 DomainDraft：\n" + context.getQuery();
    }

    /**
     * 当前领域专家名称，用于从系统身份层面区分具体专家。
     */
    protected abstract String getSpecialistTitle();

    /**
     * 当前领域的职责边界和产出定位。
     */
    protected abstract String getDomainPositioning();

    /**
     * 当前领域的专属分析步骤。
     */
    protected abstract String getDomainMethodology();

    /**
     * 当前领域必须优先识别的风险类型。
     */
    protected abstract String getRiskLens();
}
