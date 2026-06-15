package com.lh.manyfoot.agent.stream;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.lh.manyfoot.agent.stream.event.Complete;
import com.lh.manyfoot.agent.stream.event.ConversationEvent;
import com.lh.manyfoot.agent.stream.event.Failure;
import com.lh.manyfoot.agent.stream.event.NarrationDelta;
import com.lh.manyfoot.agent.stream.event.PhaseHint;
import com.lh.manyfoot.agent.stream.name.ForbiddenNameProvider;
import com.lh.manyfoot.agent.stream.name.SentenceBufferedNameMasker;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Slf4j
@Component
@RequiredArgsConstructor(access = AccessLevel.NONE)
public final class ConversationEventStreamFactory {

    private final NodeOutputToEventTranslator translator;
    private final ForbiddenNameProvider names;

    public ConversationEventStreamFactory(NodeOutputToEventTranslator translator, ForbiddenNameProvider names) {
        this.translator = translator;
        this.names = names;
    }

    public Flux<ConversationEvent> create(Flux<NodeOutput> raw, String sessionId) {
        SentenceBufferedNameMasker masker = new SentenceBufferedNameMasker(names.forbiddenTokens());

        return translator.translate(raw, sessionId)
                .concatMap(event -> {
                    if (event instanceof NarrationDelta narrationDelta) {
                        return emitNarration(masker.accept(narrationDelta.text()));
                    }
                    if (event instanceof PhaseHint || event instanceof Complete || event instanceof Failure) {
                        return drainBeforeEvent(masker, event);
                    }
                    return Flux.just(event);
                });
    }

    private Flux<ConversationEvent> drainBeforeEvent(SentenceBufferedNameMasker masker, ConversationEvent event) {
        String remaining = masker.forceFlush();
        if (remaining == null || remaining.isEmpty()) {
            return Flux.just(event);
        }
        return Flux.just(new NarrationDelta(remaining), event);
    }

    private Flux<ConversationEvent> emitNarration(String text) {
        if (text == null || text.isEmpty()) {
            return Flux.empty();
        }
        return Flux.just(new NarrationDelta(text));
    }
}
