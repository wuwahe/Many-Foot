package com.lh.manyfoot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lh.manyfoot.agent.impl.SupervisorAgent;
import com.lh.manyfoot.agent.stream.ConversationEventStreamFactory;
import com.lh.manyfoot.agent.stream.Phase;
import com.lh.manyfoot.agent.stream.event.Complete;
import com.lh.manyfoot.agent.stream.event.ConversationEvent;
import com.lh.manyfoot.agent.stream.event.Failure;
import com.lh.manyfoot.agent.stream.event.NarrationDelta;
import com.lh.manyfoot.agent.stream.event.PhaseHint;
import com.lh.manyfoot.controller.dto.ChatRequest;
import com.lh.manyfoot.service.SandboxContainerManager;
import com.lh.manyfoot.service.file.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ChatController SSE 流式对话测试。
 * <p>
 * 验证新的 SSE 事件契约：
 * - event: phase（阶段提示）
 * - event: message（叙述文本）
 * - event: done（完成）
 * - event: error（异常）
 * <p>
 * 使用 @WebMvcTest 隔离测试控制器层，Mock SupervisorAgent 和
 * ConversationEventStreamFactory，不依赖真实模型或沙箱。
 */
@WebMvcTest(ChatController.class)
class ChatControllerSseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SupervisorAgent supervisorAgent;

    /**
     * 沙箱管理器：stream() 方法不直接调用它，但控制器构造器需要。
     * Spring 会将此 mock 包装为 ObjectProvider<SandboxContainerManager> 注入。
     */
    @MockBean
    private SandboxContainerManager sandboxContainerManager;

    @MockBean
    private ConversationEventStreamFactory eventStreamFactory;

    @MockBean
    private FileStorageService fileStorageService;

    /**
     * 验证 SSE 流包含 phase、message、done 三种事件。
     */
    @Test
    void stream_shouldEmitPhaseMessageAndDone() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("hello");
        request.setSessionId("test-session");

        when(supervisorAgent.execute(any())).thenReturn(Flux.empty());

        Flux<ConversationEvent> events = Flux.just(
            new PhaseHint(Phase.THINKING),
            new NarrationDelta("让我先看一下这个问题"),
            new NarrationDelta("答案是 42"),
            new Complete("test-session")
        );
        when(eventStreamFactory.create(any(), any())).thenReturn(events);

        MvcResult mvcResult = mockMvc.perform(post("/api/chat/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();

        // SSE 异步事件由后台线程写入，需等待
        Thread.sleep(500);

        String sseContent = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(sseContent).contains("event:phase");
        assertThat(sseContent).contains("event:message");
        assertThat(sseContent).contains("event:done");
        assertThat(sseContent).contains("thinking");
        assertThat(sseContent).contains("让我先看一下这个问题");
        assertThat(sseContent).contains("答案是 42");
        assertThat(sseContent).contains("test-session");
    }

    /**
     * 验证 message 事件的 text 字段不含任何注册 Agent 名称。
     * <p>
     * 这是前端用户体验的关键契约：Supervisor 的叙述应保持口语化，
     * 不暴露内部智能体名称（如 Code_agent、Planner_Router_agent 等）。
     */
    @Test
    void stream_shouldNotContainAgentNames() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("hi");
        request.setSessionId("test-session-2");

        when(supervisorAgent.execute(any())).thenReturn(Flux.empty());

        List<String> forbiddenNames = List.of(
            "Supervisor_agent",
            "Planner_Router_agent",
            "Research_Retrieval_agent",
            "Document_Specialist_agent",
            "Tool_Action_Executor_agent",
            "Code_agent",
            "Chat_agent"
        );

        Flux<ConversationEvent> events = Flux.just(
            new NarrationDelta("我来帮你查一下资料，然后整理结果"),
            new Complete("test-session-2")
        );
        when(eventStreamFactory.create(any(), any())).thenReturn(events);

        MvcResult mvcResult = mockMvc.perform(post("/api/chat/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();

        Thread.sleep(500);

        String sseContent = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);

        for (String name : forbiddenNames) {
            assertThat(sseContent)
                .as("SSE 输出不应包含 Agent 名称: %s", name)
                .doesNotContain(name);
        }
        assertThat(sseContent).doesNotContain("_agent");
    }

    /**
     * 验证 Failure 事件会被转换为 SSE error 事件。
     */
    @Test
    void stream_shouldEmitErrorOnFailure() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("test error");
        request.setSessionId("test-session-3");

        when(supervisorAgent.execute(any())).thenReturn(Flux.empty());

        // 仅返回 Failure 事件（Failure 会调用 emitter.complete()，不需要再发 Complete）
        Flux<ConversationEvent> events = Flux.just(
            new Failure("系统处理失败，请稍后重试", new RuntimeException("模拟异常"))
        );
        when(eventStreamFactory.create(any(), any())).thenReturn(events);

        MvcResult mvcResult = mockMvc.perform(post("/api/chat/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();

        Thread.sleep(500);

        String sseContent = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(sseContent).contains("event:error");
        assertThat(sseContent).contains("系统处理失败，请稍后重试");
    }
}
