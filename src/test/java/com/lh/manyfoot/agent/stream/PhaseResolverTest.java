package com.lh.manyfoot.agent.stream;

import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.lh.manyfoot.agent.stream.event.PhaseHint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link PhaseResolver} 单元测试。
 * <p>
 * 覆盖状态机各分支：首次文本推断 THINKING、toolCall 推断 ACTING、
 * 工具执行后文本推断 RESPONDING、阶段去重、outputType 缺失兜底等场景。
 */
class PhaseResolverTest {

    @Test
    @DisplayName("首次 AGENT_MODEL_STREAMING 有文本无 toolCalls → THINKING")
    void firstModelStreamingWithoutTool_shouldEmitThinking() {
        PhaseResolver resolver = new PhaseResolver();
        AssistantMessage message = new AssistantMessage("我正在思考这个问题");

        Optional<PhaseHint> result = resolver.resolve(OutputType.AGENT_MODEL_STREAMING, message);

        assertTrue(result.isPresent(), "首次有文本应返回 PhaseHint");
        assertEquals(Phase.THINKING, result.get().phase(), "首次无 tool 的文本流应为 THINKING");
    }

    @Test
    @DisplayName("AGENT_MODEL_STREAMING 有 toolCalls → ACTING")
    void modelStreamingWithToolCalls_shouldEmitActing() {
        PhaseResolver resolver = new PhaseResolver();
        AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall(
                "call_001", "function", "sandbox_execute_code", "{\"code\":\"print(1)\"}"
        );
        AssistantMessage message = AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(toolCall))
                .build();

        Optional<PhaseHint> result = resolver.resolve(OutputType.AGENT_MODEL_STREAMING, message);

        assertTrue(result.isPresent(), "有 toolCalls 应返回 PhaseHint");
        assertEquals(Phase.ACTING, result.get().phase(), "有 toolCalls 的流应为 ACTING");
    }

    @Test
    @DisplayName("AGENT_TOOL_STREAMING → ACTING")
    void toolStreaming_shouldEmitActing() {
        PhaseResolver resolver = new PhaseResolver();

        Optional<PhaseHint> result = resolver.resolve(OutputType.AGENT_TOOL_STREAMING, null);

        assertTrue(result.isPresent(), "工具流应返回 PhaseHint");
        assertEquals(Phase.ACTING, result.get().phase(), "工具流应为 ACTING");
    }

    @Test
    @DisplayName("AGENT_MODEL_FINISHED → Optional.empty()")
    void modelFinished_shouldNotSwitch() {
        PhaseResolver resolver = new PhaseResolver();
        resolver.resolve(OutputType.AGENT_MODEL_STREAMING, new AssistantMessage("一些思考"));

        Optional<PhaseHint> result = resolver.resolve(OutputType.AGENT_MODEL_FINISHED, null);

        assertTrue(result.isEmpty(), "AGENT_MODEL_FINISHED 不应触发阶段切换");
    }

    @Test
    @DisplayName("经历 tool 后，AGENT_MODEL_STREAMING 有文本 → RESPONDING")
    void afterToolSeen_modelStreamingText_shouldEmitResponding() {
        PhaseResolver resolver = new PhaseResolver();
        AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall(
                "call_001", "function", "sandbox_execute_code", "{\"code\":\"print(1)\"}"
        );
        resolver.resolve(OutputType.AGENT_MODEL_STREAMING,
                AssistantMessage.builder().content("").toolCalls(List.of(toolCall)).build());
        resolver.resolve(OutputType.AGENT_TOOL_STREAMING, null);

        Optional<PhaseHint> result = resolver.resolve(
                OutputType.AGENT_MODEL_STREAMING,
                new AssistantMessage("执行结果为 1")
        );

        assertTrue(result.isPresent(), "经历过 tool 后的文本流应返回 PhaseHint");
        assertEquals(Phase.RESPONDING, result.get().phase(), "经历 tool 后的文本流应为 RESPONDING");
    }

    @Test
    @DisplayName("连续两次 THINKING → 只返回一次 PhaseHint")
    void samePhaseTwice_shouldDeduplicate() {
        PhaseResolver resolver = new PhaseResolver();
        AssistantMessage message1 = new AssistantMessage("第一部分思考");
        AssistantMessage message2 = new AssistantMessage("第二部分思考");

        Optional<PhaseHint> first = resolver.resolve(OutputType.AGENT_MODEL_STREAMING, message1);
        Optional<PhaseHint> second = resolver.resolve(OutputType.AGENT_MODEL_STREAMING, message2);

        assertTrue(first.isPresent(), "首次应返回 PhaseHint");
        assertEquals(Phase.THINKING, first.get().phase());
        assertTrue(second.isEmpty(), "同阶段重复不应再次返回 PhaseHint");
    }

    @Test
    @DisplayName("outputType=null, 有文本无 toolCalls → THINKING")
    void nullOutputType_withText_shouldDeduceThinking() {
        PhaseResolver resolver = new PhaseResolver();
        AssistantMessage message = new AssistantMessage("输出类型缺失时的文本");

        Optional<PhaseHint> result = resolver.resolve(null, message);

        assertTrue(result.isPresent(), "outputType 为 null 但有文本时应返回 PhaseHint");
        assertEquals(Phase.THINKING, result.get().phase(), "无 tool 的文本应为 THINKING");
    }

    @Test
    @DisplayName("outputType=null, 有文本, seenTool=true → RESPONDING")
    void nullOutputType_withTextAfterTool_shouldDeduceResponding() {
        PhaseResolver resolver = new PhaseResolver();
        AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall(
                "call_001", "function", "sandbox_execute_code", "{\"code\":\"print(1)\"}"
        );
        resolver.resolve(OutputType.AGENT_MODEL_STREAMING,
                AssistantMessage.builder().content("").toolCalls(List.of(toolCall)).build());

        Optional<PhaseHint> result = resolver.resolve(null, new AssistantMessage("总结输出"));

        assertTrue(result.isPresent(), "outputType 为 null 且 seenTool=true 时应返回 PhaseHint");
        assertEquals(Phase.RESPONDING, result.get().phase(), "经历 tool 后的文本应为 RESPONDING");
    }
}
