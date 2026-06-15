package com.lh.manyfoot.agent.stream;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.lh.manyfoot.agent.stream.event.Complete;
import com.lh.manyfoot.agent.stream.event.ConversationEvent;
import com.lh.manyfoot.agent.stream.event.NarrationDelta;
import com.lh.manyfoot.agent.stream.event.PhaseHint;
import com.lh.manyfoot.agent.stream.name.ForbiddenNameProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ConversationEventStreamFactoryTest {

    private static final String SESSION_ID = "test-session-456";

    private final NodeOutputToEventTranslator translator = new NodeOutputToEventTranslator();
    private final ConversationEventStreamFactory factory = new ConversationEventStreamFactory(translator, new StubForbiddenNameProvider());

    @Test
    @DisplayName("端到端：NarrationDelta 不应包含禁词")
    void narrationDelta_shouldNotContainForbiddenNames() {
        Flux<NodeOutput> raw = Flux.just(
                streamingOutput("Code_agent is processing your request.", OutputType.AGENT_MODEL_STREAMING)
        );

        StepVerifier.create(factory.create(raw, SESSION_ID))
                .expectNext(new PhaseHint(Phase.THINKING))
                .assertNext(event -> {
                    NarrationDelta delta = (NarrationDelta) event;
                    String text = delta.text();
                    assertFalse(text.contains("Code_agent"), "NarrationDelta 不应包含禁词 Code_agent: " + text);
                    assertFalse(text.contains("Supervisor_agent"), "NarrationDelta 不应包含禁词 Supervisor_agent: " + text);
                    assertEquals("一个内部步骤 is processing your request.", text);
                })
                .expectNext(new Complete(SESSION_ID))
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("PhaseHint(ACTING) 之前应先将 masker 中缓冲的文本 drain 完毕")
    void phaseHintActing_shouldDrainMaskerFirst() {
        AssistantMessage toolMessage = AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall("1", "function", "test", "{}")))
                .build();

        Flux<NodeOutput> raw = Flux.just(
                streamingOutput("buffered text without sentence end", OutputType.AGENT_MODEL_STREAMING),
                new StreamingOutput<>(toolMessage, "node", "agent", null, OutputType.AGENT_MODEL_STREAMING)
        );

        StepVerifier.create(factory.create(raw, SESSION_ID))
                .expectNext(new PhaseHint(Phase.THINKING))
                .assertNext(event -> {
                    assertEquals(new NarrationDelta("buffered text without sentence end"), event);
                })
                .assertNext(event -> {
                    assertEquals(new PhaseHint(Phase.ACTING), event);
                })
                .expectNext(new Complete(SESSION_ID))
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Complete 事件前应 drain 所有剩余文本")
    void completeEvent_shouldDrainRemainingText() {
        Flux<NodeOutput> raw = Flux.just(
                streamingOutput("remaining text without sentence end", OutputType.AGENT_MODEL_STREAMING)
        );

        StepVerifier.create(factory.create(raw, SESSION_ID))
                .expectNext(new PhaseHint(Phase.THINKING))
                .assertNext(event -> {
                    assertEquals(new NarrationDelta("remaining text without sentence end"), event);
                })
                .expectNext(new Complete(SESSION_ID))
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("空流应仅发射 Complete 事件")
    void emptyStream_shouldEmitOnlyComplete() {
        Flux<NodeOutput> raw = Flux.empty();

        StepVerifier.create(factory.create(raw, SESSION_ID))
                .expectNext(new Complete(SESSION_ID))
                .expectComplete()
                .verify();
    }

    private NodeOutput streamingOutput(String text, OutputType type) {
        return new StreamingOutput<>(new AssistantMessage(text), "node", "agent", null, type);
    }

    private static class StubForbiddenNameProvider implements ForbiddenNameProvider {
        @Override
        public Set<String> forbiddenTokens() {
            return Set.of(Pattern.quote("Code_agent"), Pattern.quote("Supervisor_agent"));
        }
    }
}
