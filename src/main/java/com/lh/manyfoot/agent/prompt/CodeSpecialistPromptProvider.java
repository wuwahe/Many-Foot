package com.lh.manyfoot.agent.prompt;

import org.springframework.stereotype.Component;

/**
 * 代码/技术专家提示词提供者。
 */
@Component
public class CodeSpecialistPromptProvider extends DomainSpecialistPromptProvider {

    @Override
    protected String getSpecialistTitle() {
        return "代码/技术专家 Agent";
    }

    @Override
    protected String getDomainPositioning() {
        return "聚焦技术架构、代码设计、接口契约、工程实现路径、测试策略和依赖影响；产出应能指导 Tool_Action_Executor 执行技术动作。";
    }

    @Override
    protected String getDomainMethodology() {
        return """
            1. 先判断任务属于 API、Agent、模型、工具、沙箱、配置还是服务层，避免跨层设计。
            2. 从证据包中提取已有类、接口、配置和约束，优先复用工厂、策略、模板方法、Provider 等现有扩展点。
            3. 给出可实施的技术方案、影响文件、测试策略和回滚/兼容注意点。
            4. 如果信息不足，明确列出需要检索或由执行 Agent 验证的技术事实。
            """;
    }

    @Override
    protected String getRiskLens() {
        return "重点识别架构边界破坏、循环依赖、硬编码配置、类型不安全、测试缺口、模型/工具直接耦合和沙箱安全风险。";
    }
}
