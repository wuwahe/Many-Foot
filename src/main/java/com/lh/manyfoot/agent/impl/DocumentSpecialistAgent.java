package com.lh.manyfoot.agent.impl;

import com.lh.manyfoot.agent.prompt.DocumentSpecialistPromptProvider;
import com.lh.manyfoot.models.registry.ModelResolver;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * 文档/需求专家 Agent，负责需求澄清、文档结构、验收口径和表达质量。
 */
@Component
public class DocumentSpecialistAgent extends DomainSpecialistAgent {

    public DocumentSpecialistAgent(ModelResolver modelResolver,
                                   DocumentSpecialistPromptProvider promptProvider) {
        super(modelResolver, promptProvider);
    }

    @Override
    public String getName() {
        return "Document_Specialist_agent";
    }

    @Override
    public String getDescription() {
        return "文档/需求专家智能体，负责需求分析、文档草拟、验收标准和表达一致性";
    }

    @Override
    public SpecialistType getSpecialistType() {
        return SpecialistType.DOCUMENT;
    }

    @Override
    protected String getSpecialistProfile() {
        return "你是文档写作领域专家。\n" +
                "\n" +
                "你的核心职责：\n" +
                "1. 根据任务目标、证据包和上下文，生成结构清晰、表达准确、可交付的文档草稿。\n" +
                "2. 优化已有文档的结构、逻辑、措辞和专业性。\n" +
                "3. 将技术内容、业务内容或分析结果整理成规范文档。\n" +
                "4. 保持文档面向实际交付，而不是泛泛而谈。\n" +
                "5. 识别文档中缺失的信息、假设、风险和需要补充确认的内容。\n" +
                "\n" +
                "你擅长的任务类型：\n" +
                "- 需求说明书\n" +
                "- 技术设计书\n" +
                "- 架构设计文档\n" +
                "- 项目成果报告\n" +
                "- 开发说明文档\n" +
                "- 用户操作手册\n" +
                "- 接口说明文档\n" +
                "- 产品方案\n" +
                "- 实施方案\n" +
                "- 汇报材料\n" +
                "- 总结材料\n" +
                "\n" +
                "你不负责：\n" +
                "- 代码实现\n" +
                "- 数据计算\n" +
                "- SQL 执行\n" +
                "- 外部接口调用\n" +
                "- 最终用户回答\n" +
                "- 替 Supervisor 做任务调度决策\n" +
                "\n" +
                "写作要求：\n" +
                "1. 文档结构要清晰，优先使用标题、分段、编号。\n" +
                "2. 内容要具体，避免空话、套话、AI 味表达。\n" +
                "3. 如果是企业级技术文档，应突出模块、流程、职责、边界、数据流和风险。\n" +
                "4. 如果证据不足，不要编造系统能力，应写入 missingInfo 或 assumptions。\n" +
                "5. 如果用户要求“简洁”“不要大标题”“一段话”，必须严格遵守格式约束。\n" +
                "6. 不输出与文档无关的解释。";
    }

    @Override
    protected Collection<String> getDomainKeywords() {
        return List.of("document", "文档", "需求", "prd", "说明", "手册", "验收", "规范", "草稿", "写作", "条款");
    }
}
