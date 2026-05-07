package com.lh.manyfoot.models;

import com.lh.manyfoot.config.properties.AiProvidersProperties;
import com.lh.manyfoot.config.properties.AiProvidersProperties.Binding;
import com.lh.manyfoot.models.failover.FailoverChatModel;
import com.lh.manyfoot.models.registry.ModelResolver;
import com.lh.manyfoot.models.registry.ModelRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.retry.TransientAiException;
import reactor.core.publisher.Flux;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 纯 JUnit 单测：不起 Spring 上下文，专门验证 {@link ModelResolver} + {@link FailoverChatModel}
 * 在 Supervisor 专业智能体角色链路上的路由和降级行为。
 */
class ModelResolverFailoverTest {

    /** 一个可控的假 ChatModel：可以指定每次 call 是否抛异常。 */
    private static class FakeChatModel implements ChatModel {
        final String tag;
        private final AtomicInteger calls = new AtomicInteger();
        RuntimeException nextError;

        FakeChatModel(String tag) {
            this.tag = tag;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            calls.incrementAndGet();
            if (nextError != null) {
                RuntimeException e = nextError;
                throw e;
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage(tag))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }

        int callCount() {
            return calls.get();
        }
    }

    private ModelResolver resolver;
    private FakeChatModel qwenMax;
    private FakeChatModel qwenPlus;
    private FakeChatModel qwenCoder;

    @BeforeEach
    void setUp() {
        resolver = new ModelResolver();
        qwenMax = new FakeChatModel("qwen-max");
        qwenPlus = new FakeChatModel("qwen-plus");
        qwenCoder = new FakeChatModel("qwen3-coder");

        resolver.registerChatModel("qwen-max", qwenMax);
        resolver.registerChatModel("qwen-plus", qwenPlus);
        resolver.registerChatModel("qwen3-coder", qwenCoder);

        Map<ModelRole, Binding> roles = new EnumMap<>(ModelRole.class);
        roles.put(ModelRole.PLANNER_ROUTER, binding("qwen-max", List.of("qwen-plus")));
        roles.put(ModelRole.RESEARCH_RETRIEVAL, binding("qwen-max", List.of("qwen-plus")));
        roles.put(ModelRole.DOMAIN_SPECIALIST, binding("qwen-max", List.of("qwen-plus")));
        roles.put(ModelRole.TOOL_ACTION_EXECUTOR, binding("qwen3-coder", List.of("qwen-max")));
        roles.put(ModelRole.CRITIC_VERIFIER, binding("qwen-plus", List.of("qwen-max")));
        resolver.bindRoles(roles);

        resolver.setDefaultProviderId("qwen-max");
    }

    @Test
    void forRole_shouldReturnPrimary_onHappyPath() {
        // 五类专业智能体角色分别拿到对应 primary
        assertEquals("qwen-max", textOf(resolver.forRole(ModelRole.PLANNER_ROUTER)));
        assertEquals("qwen-max", textOf(resolver.forRole(ModelRole.RESEARCH_RETRIEVAL)));
        assertEquals("qwen-max", textOf(resolver.forRole(ModelRole.DOMAIN_SPECIALIST)));
        assertEquals("qwen3-coder", textOf(resolver.forRole(ModelRole.TOOL_ACTION_EXECUTOR)));
        assertEquals("qwen-plus", textOf(resolver.forRole(ModelRole.CRITIC_VERIFIER)));
    }

    @Test
    void forRole_shouldFailoverOnTransientError() {
        qwenCoder.nextError = new TransientAiException("simulated 429");
        ChatResponse resp = resolver.forRole(ModelRole.TOOL_ACTION_EXECUTOR).call(new Prompt("hi"));
        assertEquals("qwen-max", assistant(resp));
        assertEquals(1, qwenCoder.callCount(), "primary 应被调用一次");
        assertEquals(1, qwenMax.callCount(), "fallback 应接管一次");
    }

    @Test
    void forRole_shouldNotRetryOnBusinessError() {
        qwenCoder.nextError = new IllegalArgumentException("bad prompt");
        assertThrows(IllegalArgumentException.class,
            () -> resolver.forRole(ModelRole.TOOL_ACTION_EXECUTOR).call(new Prompt("hi")));
        assertEquals(0, qwenMax.callCount(), "业务异常不应触发 fallback");
    }

    @Test
    void forAgent_shouldOverrideRole() {
        AiProvidersProperties.AgentBinding ab = new AiProvidersProperties.AgentBinding();
        ab.setRole(ModelRole.TOOL_ACTION_EXECUTOR);
        ab.setPrimary("qwen-max");
        ab.setFallbacks(List.of("qwen-plus"));
        resolver.bindAgents(Map.of("Tool_Action_Executor_agent", ab));

        // 虽然 TOOL_ACTION_EXECUTOR 的 role 绑定是 qwen3-coder，但 agent 覆盖指向 qwen-max
        ChatModel m = resolver.forAgent("Tool_Action_Executor_agent", ModelRole.TOOL_ACTION_EXECUTOR);
        assertEquals("qwen-max", textOf(m));
        // 非覆盖 agent 仍走 role
        assertEquals("qwen3-coder", textOf(resolver.forAgent("Unknown_agent", ModelRole.TOOL_ACTION_EXECUTOR)));
    }

    @Test
    void forAgent_shouldFallbackToDefaultWhenRoleUnbound() {
        resolver.bindRoles(Map.of()); // 清空 role 绑定
        ChatModel m = resolver.forAgent("Any", ModelRole.PLANNER_ROUTER);
        assertNotNull(m);
        // 默认 qwen-max
        assertEquals("qwen-max", textOf(m));
    }

    @Test
    void supportsMultimodalForAgent_shouldUseResolvedPrimaryProvider() {
        resolver.registerChatModel("vision", new FakeChatModel("vision"), true);
        resolver.registerChatModel("text", new FakeChatModel("text"), false);

        Binding roleBinding = binding("text", List.of("vision"));
        resolver.bindRoles(Map.of(ModelRole.CHAT, roleBinding));

        assertFalse(resolver.supportsMultimodalForAgent("Chat_agent", ModelRole.CHAT));

        AiProvidersProperties.AgentBinding agentBinding = new AiProvidersProperties.AgentBinding();
        agentBinding.setPrimary("vision");
        resolver.bindAgents(Map.of("Chat_agent", agentBinding));

        assertTrue(resolver.supportsMultimodalForAgent("Chat_agent", ModelRole.CHAT));
    }

    // ---------- 工具方法 ----------

    private static Binding binding(String primary, List<String> fallbacks) {
        Binding b = new Binding();
        b.setPrimary(primary);
        b.setFallbacks(fallbacks);
        return b;
    }

    private static String textOf(ChatModel model) {
        return assistant(model.call(new Prompt("ping")));
    }

    private static String assistant(ChatResponse r) {
        return r.getResult().getOutput().getText();
    }
}
