package com.lh.manyfoot.models.failover;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 故障转移 ChatModel 装饰器。
 * <p>
 * 按顺序尝试 primary -> fallbacks，针对 <strong>可重试异常</strong>（限流 429、5xx、
 * 网络超时、{@link TransientAiException} 等）切到下一家，遇到 4xx（非 429）等不可重试错误
 * 直接抛出。对流式 {@link #stream(Prompt)} 仅在"首个 chunk 未到达前"切换，避免把两家的
 * 流拼接在一起造成内容错乱。
 */
@Slf4j
public class FailoverChatModel implements ChatModel {

    /** 命名过的 ChatModel 条目，name 只用于日志。 */
    public record Named(String id, ChatModel model) {
    }

    private final List<Named> chain;

    public FailoverChatModel(Named primary, List<Named> fallbacks) {
        if (primary == null || primary.model() == null) {
            throw new IllegalArgumentException("primary ChatModel 不能为空");
        }
        List<Named> list = new ArrayList<>();
        list.add(primary);
        if (fallbacks != null) {
            for (Named n : fallbacks) {
                if (n != null && n.model() != null) {
                    list.add(n);
                }
            }
        }
        this.chain = List.copyOf(list);
    }

    /**
     * 便捷工厂：chain 只有一个节点时直接返回原 ChatModel，避免无意义的装饰。
     */
    public static ChatModel of(Named primary, List<Named> fallbacks) {
        if (fallbacks == null || fallbacks.isEmpty()) {
            return primary.model();
        }
        return new FailoverChatModel(primary, fallbacks);
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        RuntimeException lastErr = null;
        for (int i = 0; i < chain.size(); i++) {
            Named n = chain.get(i);
            try {
                return n.model().call(prompt);
            } catch (RuntimeException e) {
                lastErr = e;
                if (!isRetryable(e) || i == chain.size() - 1) {
                    throw e;
                }
                log.warn("ChatModel[{}] 调用失败，降级到下一个: {} -> {}",
                    n.id(), chain.get(i + 1).id(), rootMessage(e));
            }
        }
        throw lastErr != null ? lastErr : new IllegalStateException("failover chain 为空");
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return streamFromIndex(prompt, 0);
    }

    private Flux<ChatResponse> streamFromIndex(Prompt prompt, int index) {
        Named n = chain.get(index);
        AtomicBoolean firstChunkSeen = new AtomicBoolean(false);
        Flux<ChatResponse> upstream;
        try {
            upstream = n.model().stream(prompt);
        } catch (RuntimeException e) {
            return handleStreamError(prompt, index, n, e, firstChunkSeen.get());
        }
        return upstream
            .doOnNext(r -> firstChunkSeen.set(true))
            .onErrorResume(err -> handleStreamError(prompt, index, n,
                err instanceof RuntimeException re ? re : new RuntimeException(err),
                firstChunkSeen.get()));
    }

    private Flux<ChatResponse> handleStreamError(Prompt prompt, int index, Named current,
                                                 RuntimeException e, boolean alreadyStreamed) {
        boolean canRetry = isRetryable(e) && index + 1 < chain.size() && !alreadyStreamed;
        if (!canRetry) {
            return Flux.error(e);
        }
        log.warn("ChatModel[{}] 流式调用失败，降级到 [{}]: {}",
            current.id(), chain.get(index + 1).id(), rootMessage(e));
        return streamFromIndex(prompt, index + 1);
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return chain.get(0).model().getDefaultOptions();
    }

    /**
     * 判断异常是否可重试（语义：当前模型临时不可用，换一家可能成功）。
     * <ul>
     *     <li>Spring AI 抛出的 {@link TransientAiException} 一律可重试</li>
     *     <li>{@link NonTransientAiException} 一律不重试</li>
     *     <li>HTTP 429 / 5xx 可重试</li>
     *     <li>{@link IOException} / {@link SocketTimeoutException} / {@link TimeoutException} 可重试</li>
     *     <li>其他默认不重试（避免把业务错误掩盖）</li>
     * </ul>
     */
    private boolean isRetryable(Throwable e) {
        Throwable cur = e;
        while (cur != null) {
            if (cur instanceof NonTransientAiException) {
                return false;
            }
            if (cur instanceof TransientAiException) {
                return true;
            }
            if (cur instanceof HttpClientErrorException.TooManyRequests) {
                return true;
            }
            if (cur instanceof HttpServerErrorException) {
                return true;
            }
            if (cur instanceof SocketTimeoutException
                || cur instanceof TimeoutException
                || cur instanceof IOException) {
                return true;
            }
            cur = cur.getCause() == cur ? null : cur.getCause();
        }
        return false;
    }

    private static String rootMessage(Throwable e) {
        Throwable cur = e;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur.getClass().getSimpleName() + ": " + cur.getMessage();
    }
}
