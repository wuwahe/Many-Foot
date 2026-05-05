package com.lh.manyfoot.agent.prompt;

import com.lh.manyfoot.agent.context.AgentContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证具体领域专家使用不同系统提示词，而不是只共享一份通用提示词。
 */
class DomainSpecialistPromptProviderTest {

    private final AgentContext context = AgentContext.builder()
        .sessionId("session-1")
        .additionalContext("## 当前具体领域专家\n- 类型：TEST")
        .query("{}")
        .build();

    @Test
    void buildSystemPrompt_shouldUseCodeSpecificPrompt() {
        String prompt = new CodeSpecialistPromptProvider().buildSystemPrompt(context);

        assertTrue(prompt.contains("代码/技术专家 Agent"));
        assertTrue(prompt.contains("技术架构"));
        assertTrue(prompt.contains("测试策略"));
        assertTrue(prompt.contains("架构边界破坏"));
    }

    @Test
    void buildSystemPrompt_shouldUseBusinessSpecificPrompt() {
        String prompt = new BusinessSpecialistPromptProvider().buildSystemPrompt(context);

        assertTrue(prompt.contains("通用业务专家 Agent"));
        assertTrue(prompt.contains("业务目标"));
        assertTrue(prompt.contains("流程边界"));
        assertTrue(prompt.contains("验收口径模糊"));
    }

    @Test
    void buildSystemPrompt_shouldUseDocumentSpecificPrompt() {
        String prompt = new DocumentSpecialistPromptProvider().buildSystemPrompt(context);

        assertTrue(prompt.contains("文档/需求专家 Agent"));
        assertTrue(prompt.contains("文档结构"));
        assertTrue(prompt.contains("目标读者"));
        assertTrue(prompt.contains("术语混用"));
    }

    @Test
    void buildSystemPrompt_shouldUseDataSpecificPrompt() {
        String prompt = new DataSpecialistPromptProvider().buildSystemPrompt(context);

        assertTrue(prompt.contains("数据/SQL/分析专家 Agent"));
        assertTrue(prompt.contains("指标口径"));
        assertTrue(prompt.contains("时间窗口"));
        assertTrue(prompt.contains("慢查询"));
    }

    @Test
    void buildSystemPrompt_shouldDifferAcrossConcreteProviders() {
        String codePrompt = new CodeSpecialistPromptProvider().buildSystemPrompt(context);
        String businessPrompt = new BusinessSpecialistPromptProvider().buildSystemPrompt(context);
        String documentPrompt = new DocumentSpecialistPromptProvider().buildSystemPrompt(context);
        String dataPrompt = new DataSpecialistPromptProvider().buildSystemPrompt(context);

        assertNotEquals(codePrompt, businessPrompt);
        assertNotEquals(codePrompt, documentPrompt);
        assertNotEquals(codePrompt, dataPrompt);
        assertNotEquals(businessPrompt, documentPrompt);
        assertNotEquals(businessPrompt, dataPrompt);
        assertNotEquals(documentPrompt, dataPrompt);
    }
}
