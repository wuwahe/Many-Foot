package com.lh.manyfoot.agent.impl;

import com.lh.manyfoot.agent.prompt.ResearchRetrievalPromptProvider;
import com.lh.manyfoot.agent.tool.FullToolProvider;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ResearchRetrievalAgent 成本控制回归测试。
 */
class ResearchRetrievalAgentTest {

    @Test
    void getAvailableTools_shouldOnlyExposeSearchTool() {
        ResearchRetrievalAgent agent = new ResearchRetrievalAgent(null, null, null);

        Set<String> tools = agent.getAvailableTools();

        assertEquals(Set.of("webSearch", "deepResearch", "batchFetchPages", "webFetchPage", "newsSearch"), tools);
    }

    @Test
    void buildSystemPrompt_shouldContainSearchBudget() {
        ResearchRetrievalPromptProvider provider = new ResearchRetrievalPromptProvider();

        String prompt = provider.buildSystemPrompt(
                com.lh.manyfoot.agent.context.AgentContext.builder()
                        .sessionId("test-session")
                        .query("test")
                        .build()
        );

        assertTrue(prompt.contains("每次任务最多调用 3 次搜索工具"));
        assertTrue(prompt.contains("不要展开子链接或解析网页正文"));
    }

    @Test
    void getTools_shouldLimitSearchCallsPerExecution() {
        AtomicInteger delegateCalls = new AtomicInteger();
        ToolCallback searchTool = searchTool(delegateCalls);
        FullToolProvider toolProvider = toolProvider(searchTool);
        ResearchRetrievalAgent agent = new ResearchRetrievalAgent(null, null, toolProvider);

        ToolCallback limitedSearchTool = agent.getTools().get(0);

        assertEquals("search-result-1", limitedSearchTool.call("{}"));
        assertEquals("search-result-2", limitedSearchTool.call("{}"));
        assertEquals("search-result-3", limitedSearchTool.call("{}"));
        assertTrue(limitedSearchTool.call("{}").contains("搜索预算已用完"));
        assertEquals(3, delegateCalls.get());
    }

    private FullToolProvider toolProvider(ToolCallback toolCallback) {
        ToolCallbackProvider callbackProvider = new ToolCallbackProvider() {
            @Override
            public ToolCallback[] getToolCallbacks() {
                return new ToolCallback[]{toolCallback};
            }
        };
        return new FullToolProvider(List.of(callbackProvider));
    }

    private ToolCallback searchTool(AtomicInteger delegateCalls) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder()
                        .name("webSearch")
                        .description("test search")
                        .inputSchema("{}")
                        .build();
            }

            @Override
            public String call(String toolInput) {
                return "search-result-" + delegateCalls.incrementAndGet();
            }

            @Override
            public String call(String toolInput, ToolContext toolContext) {
                return call(toolInput);
            }
        };
    }
}
