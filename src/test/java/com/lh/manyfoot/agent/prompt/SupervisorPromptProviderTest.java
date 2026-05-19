package com.lh.manyfoot.agent.prompt;

import com.lh.manyfoot.agent.context.AgentContext;
import com.lh.manyfoot.agent.core.Agent;
import com.lh.manyfoot.agent.registry.AgentRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SupervisorPromptProvider 回归测试。
 * <p>
 * 验证提示词核心纪律段落和动态 Agent 列表生成逻辑，
 * 确保 Supervisor 的系统提示词始终包含叙述风格约束和可用智能体清单。
 */
@ExtendWith(MockitoExtension.class)
class SupervisorPromptProviderTest {

    @Mock
    private AgentRegistry agentRegistry;

    private SupervisorPromptProvider provider;

    @BeforeEach
    void setUp() {
        provider = new SupervisorPromptProvider(agentRegistry);
    }

    @Test
    void buildSystemPrompt_shouldContainNarrationDiscipline() {
        when(agentRegistry.getAgentsExcluding("Supervisor_agent")).thenReturn(List.of());

        String prompt = provider.buildSystemPrompt(
            AgentContext.builder().sessionId("test").query("hi").build()
        );

        assertTrue(prompt.contains("# 叙述风格（最高优先级）"));
    }

    @Test
    void buildSystemPrompt_shouldContainForbiddenNameSection() {
        when(agentRegistry.getAgentsExcluding("Supervisor_agent")).thenReturn(List.of());

        String prompt = provider.buildSystemPrompt(
            AgentContext.builder().sessionId("test").query("hi").build()
        );

        assertTrue(prompt.contains("## 严禁暴露内部组件名"));
    }

    @Test
    void buildSystemPrompt_shouldContainRhythmSection() {
        when(agentRegistry.getAgentsExcluding("Supervisor_agent")).thenReturn(List.of());

        String prompt = provider.buildSystemPrompt(
            AgentContext.builder().sessionId("test").query("hi").build()
        );

        assertTrue(prompt.contains("## 节奏：动手前"));
    }

    @Test
    void buildSystemPrompt_shouldContainAllAgentNames() {
        List<Agent<String>> agents = List.of(
            mockAgent("Planner_Router_agent"),
            mockAgent("Research_Retrieval_agent"),
            mockAgent("Document_Specialist_agent"),
            mockAgent("Tool_Action_Executor_agent"),
            mockAgent("Code_agent"),
            mockAgent("Chat_agent"),
            mockAgent("Unique_Test_agent")
        );

        when(agentRegistry.getAgentsExcluding("Supervisor_agent")).thenReturn(agents);

        String prompt = provider.buildSystemPrompt(
            AgentContext.builder().sessionId("test").query("hi").build()
        );

        for (Agent<String> agent : agents) {
            assertTrue(prompt.contains(agent.getName()),
                "提示词应包含 Agent 名称: " + agent.getName());
        }
    }

    private Agent<String> mockAgent(String name) {
        Agent<String> agent = mock(Agent.class);
        when(agent.getName()).thenReturn(name);
        when(agent.getDescription()).thenReturn("测试智能体");
        return agent;
    }
}
