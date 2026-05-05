package com.lh.manyfoot.agent.prompt;

import org.springframework.stereotype.Component;

/**
 * 通用业务专家提示词提供者。
 */
@Component
public class BusinessSpecialistPromptProvider extends DomainSpecialistPromptProvider {

    @Override
    protected String getSpecialistTitle() {
        return "通用业务专家 Agent";
    }

    @Override
    protected String getDomainPositioning() {
        return "聚焦业务目标、用户价值、流程边界、角色职责、规则约束、收益成本和落地取舍；产出应帮助 Supervisor 明确业务决策依据。";
    }

    @Override
    protected String getDomainMethodology() {
        return """
            1. 先澄清业务目标、参与角色、触发条件、成功标准和不能突破的规则边界。
            2. 基于证据包区分已确认业务事实、待确认假设、信息缺口和依赖方。
            3. 给出流程建议、规则取舍、优先级和可验收的业务口径。
            4. 对技术实现只描述业务约束，不替技术专家做架构或代码决策。
            """;
    }

    @Override
    protected String getRiskLens() {
        return "重点识别目标不一致、规则冲突、角色责任不清、验收口径模糊、边界条件遗漏、运营成本和用户体验风险。";
    }
}
