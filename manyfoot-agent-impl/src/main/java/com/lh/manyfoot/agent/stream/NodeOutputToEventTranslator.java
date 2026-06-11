package com.lh.manyfoot.agent.stream;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.lh.manyfoot.agent.stream.event.Complete;
import com.lh.manyfoot.agent.stream.event.ConversationEvent;
import com.lh.manyfoot.agent.stream.event.Failure;
import com.lh.manyfoot.agent.stream.event.NarrationDelta;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public final class NodeOutputToEventTranslator {

    public Flux<ConversationEvent> translate(Flux<NodeOutput> source, String sessionId) {
        PhaseResolver phaseResolver = new PhaseResolver();

        return source
                .concatMap(nodeOutput -> {
                    if (!(nodeOutput instanceof StreamingOutput<?> streamingOutput)) {
                        return Flux.empty();
                    }

                    OutputType outputType = streamingOutput.getOutputType();
                    AssistantMessage message = streamingOutput.message() instanceof AssistantMessage assistantMessage
                            ? assistantMessage
                            : null;

                    List<ConversationEvent> events = new ArrayList<>();

                    // 阶段提示必须先于文本增量，前端才能在展示文本前切换正确状态。
                    phaseResolver.resolve(outputType, message).ifPresent(events::add);

                    if (message == null || message.hasToolCalls()) {
                        return Flux.fromIterable(events);
                    }

                    String text = message.getText();
                    if (text != null && !text.isBlank()) {
                        events.add(new NarrationDelta(text));
                    }

                    return Flux.fromIterable(events);
                })
                .concatWith(Flux.just(new Complete(sessionId)))
                .onErrorResume(error -> Flux.just(new Failure("流式对话异常", error)));
    }
}
