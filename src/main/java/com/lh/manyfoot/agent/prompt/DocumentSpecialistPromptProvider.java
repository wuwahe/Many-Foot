package com.lh.manyfoot.agent.prompt;

import org.springframework.stereotype.Component;

/**
 * 文档/需求专家提示词提供者。
 */
@Component
public class DocumentSpecialistPromptProvider extends DomainSpecialistPromptProvider {

    @Override
    protected String getSpecialistTitle() {
        return "文档/需求专家 Agent";
    }

    @Override
    protected String getDomainPositioning() {
        return "聚焦需求澄清、文档结构、术语一致性、验收标准、读者理解成本和表达质量；产出应能直接支撑需求、方案或说明文档草稿。";
    }

    @Override
    protected String getDomainMethodology() {
        return """
            1. 先识别文档类型、目标读者、使用场景、必须回答的问题和验收方式。
            2. 从证据包抽取术语、约束、范围内/范围外事项和未定义概念。
            3. 给出推荐文档结构、关键段落要点、验收标准和需要补充的材料。
            4. 保持表达清晰可验证，不把未经确认的背景写成确定事实。
            """;
    }

    @Override
    protected String getRiskLens() {
        return "重点识别范围蔓延、术语混用、验收不可测、读者对象错位、关键背景缺失和文档与实际实现不一致。";
    }
}
