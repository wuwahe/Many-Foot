package com.lh.manyfoot.agent.prompt;

import com.lh.manyfoot.agent.context.AgentContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainSpecialistPromptProviderTest {

    private final AgentContext context = AgentContext.builder()
        .sessionId("session-1")
        .additionalContext("## 当前具体领域专家\n- 类型：TEST")
        .query("{}")
        .build();

    @Test
    void buildSystemPrompt_shouldUseDocumentSpecificPrompt() {
        String prompt = new DocumentSpecialistPromptProvider().buildSystemPrompt(context);

        assertTrue(prompt.contains("文档/需求专家 Agent"));
        assertTrue(prompt.contains("文档结构"));
        assertTrue(prompt.contains("目标读者"));
        assertTrue(prompt.contains("术语混用"));
    }
}
