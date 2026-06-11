package com.lh.manyfoot.agent.stream;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.lh.manyfoot.agent.stream.event.Complete;
import com.lh.manyfoot.agent.stream.event.ConversationEvent;
import com.lh.manyfoot.agent.stream.event.Failure;
import com.lh.manyfoot.agent.stream.event.NarrationDelta;
import com.lh.manyfoot.agent.stream.event.PhaseHint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NodeOutputToEventTranslatorTest {

    private static final String SESSION_ID = "test-session-123";

    private final NodeOutputToEventTranslator translator = new NodeOutputToEventTranslator();

    @Test
    @DisplayName("模型流式文本应发射阶段提示和叙述增量")
    void modelStreamingText_shouldEmitPhaseHintAndNarrationDelta() {
        Flux<NodeOutput> source = Flux.just(
                streamingOutput("hello", OutputType.AGENT_MODEL_STREAMING)
        );

        StepVerifier.create(translator.translate(source, SESSION_ID))
                .assertNext(event -> {
                    assertEquals(new PhaseHint(Phase.THINKING), event);
                })
                .assertNext(event -> {
                    assertEquals(new NarrationDelta("hello"), event);
                })
                .assertNext(event -> {
                    assertEquals(new Complete(SESSION_ID), event);
                })
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("空白文本应仅发射阶段提示，丢弃叙述增量")
    void emptyText_shouldBeDiscarded() {
        Flux<NodeOutput> source = Flux.just(
                streamingOutput("   ", OutputType.AGENT_MODEL_STREAMING)
        );

        StepVerifier.create(translator.translate(source, SESSION_ID))
                .assertNext(event -> {
                    assertEquals(new PhaseHint(Phase.THINKING), event);
                })
                .assertNext(event -> {
                    assertEquals(new Complete(SESSION_ID), event);
                })
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("纯工具调用应仅发射 ACTING 阶段提示")
    void toolOnly_shouldEmitActingOnly() {
        AssistantMessage toolMessage = AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall("1", "function", "test_tool", "{}")))
                .build();

        Flux<NodeOutput> source = Flux.just(
                new StreamingOutput<>(toolMessage, "node", "agent", null, OutputType.AGENT_MODEL_STREAMING)
        );

        StepVerifier.create(translator.translate(source, SESSION_ID))
                .assertNext(event -> {
                    assertEquals(new PhaseHint(Phase.ACTING), event);
                })
                .assertNext(event -> {
                    assertEquals(new Complete(SESSION_ID), event);
                })
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("缺失 outputType 时应按消息形状兜底为 THINKING + NarrationDelta")
    void nullOutputType_withText_shouldFallback() {
        Flux<NodeOutput> source = Flux.just(
                streamingOutput("fallback text", null)
        );

        StepVerifier.create(translator.translate(source, SESSION_ID))
                .assertNext(event -> {
                    assertEquals(new PhaseHint(Phase.THINKING), event);
                })
                .assertNext(event -> {
                    assertEquals(new NarrationDelta("fallback text"), event);
                })
                .assertNext(event -> {
                    assertEquals(new Complete(SESSION_ID), event);
                })
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("流正常结束应追加 Complete 事件")
    void streamCompletion_shouldEmitComplete() {
        Flux<NodeOutput> source = Flux.just(
                streamingOutput("first", OutputType.AGENT_MODEL_STREAMING),
                streamingOutput("second", OutputType.AGENT_MODEL_STREAMING)
        );

        StepVerifier.create(translator.translate(source, SESSION_ID))
                .expectNext(new PhaseHint(Phase.THINKING))
                .expectNext(new NarrationDelta("first"))
                .expectNext(new NarrationDelta("second"))
                .assertNext(event -> {
                    assertEquals(new Complete(SESSION_ID), event);
                })
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("流异常应发射 Failure 事件，不追加 Complete")
    void streamError_shouldEmitFailure() {
        RuntimeException error = new RuntimeException("stream broken");
        Flux<NodeOutput> source = Flux.just(
                streamingOutput("before error", OutputType.AGENT_MODEL_STREAMING)
        ).concatWith(Flux.error(error));

        StepVerifier.create(translator.translate(source, SESSION_ID))
                .expectNext(new PhaseHint(Phase.THINKING))
                .expectNext(new NarrationDelta("before error"))
                .assertNext(event -> {
                    assertEquals(Failure.class, event.getClass());
                    Failure failure = (Failure) event;
                    assertEquals("流式对话异常", failure.userMessage());
                    assertEquals(error, failure.cause());
                })
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("多个文本片段应保持顺序")
    void multipleTextChunks_shouldPreserveOrder() {
        Flux<NodeOutput> source = Flux.just(
                streamingOutput("one", OutputType.AGENT_MODEL_STREAMING),
                streamingOutput("two", OutputType.AGENT_MODEL_STREAMING),
                streamingOutput("three", OutputType.AGENT_MODEL_STREAMING)
        );

        StepVerifier.create(translator.translate(source, SESSION_ID))
                .expectNext(new PhaseHint(Phase.THINKING))
                .expectNext(new NarrationDelta("one"))
                .expectNext(new NarrationDelta("two"))
                .expectNext(new NarrationDelta("three"))
                .expectNext(new Complete(SESSION_ID))
                .expectComplete()
                .verify();
    }

    private NodeOutput streamingOutput(String text, OutputType type) {
        return new StreamingOutput<>(new AssistantMessage(text), "node", "agent", null, type);
    }
}
