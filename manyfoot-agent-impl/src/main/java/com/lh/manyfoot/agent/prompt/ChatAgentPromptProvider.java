package com.lh.manyfoot.agent.prompt;

import com.lh.manyfoot.agent.context.AgentContext;
import org.springframework.stereotype.Component;

@Component
public class ChatAgentPromptProvider implements AgentPromptProvider {

    @Override
    public String buildSystemPrompt(AgentContext context) {
        if (context != null && context.hasImageAttachments()) {
            return buildMultimodalSystemPrompt(context);
        }
        return "你是一个友好的对话助手，负责处理简单的日常对话和问答。直接、简洁地回答用户问题。";
    }

    private String buildMultimodalSystemPrompt(AgentContext context) {
        return """
                你是一个多模态文档分析助手，擅长理解图片、截图、文档扫描件和图表。

                ## 核心能力
                - **图片描述**：详细描述图片中的视觉内容，包括物体、场景、文字、布局
                - **文档提取**：从截图或扫描件中提取文字内容，保持原始格式
                - **图表分析**：理解图表、流程图、架构图的结构和数据含义
                - **截图解读**：分析 UI 截图、错误截图、界面设计

                ## 分析原则
                1. 先整体描述，再关注细节
                2. 提取可见的文字内容时保持准确
                3. 对于图表，说明数据趋势和关键指标
                4. 如果图片模糊或信息不完整，明确说明
                5. 用中文回答，除非用户使用其他语言

                ## 输出格式
                - 使用清晰的结构化格式
                - 重要信息用粗体标注
                - 提取的文字用引用块或代码块包裹
                - 保持回答简洁但完整
                """;
    }

    @Override
    public String buildUserInput(AgentContext context) {
        return context.getQuery();
    }
}
