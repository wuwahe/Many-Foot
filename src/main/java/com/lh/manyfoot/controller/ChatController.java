package com.lh.manyfoot.controller;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.lh.manyfoot.agent.context.AgentContext;
import com.lh.manyfoot.agent.impl.SupervisorAgent;
import com.lh.manyfoot.controller.dto.ChatRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final SupervisorAgent supervisorAgent;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);

        String sessionId = request.getSessionId() != null
                ? request.getSessionId()
                : UUID.randomUUID().toString();

        executor.execute(() -> {
            try {
                AgentContext context = AgentContext.builder()
                        .sessionId(sessionId)
                        .query(request.getMessage())
                        .build();

                log.info("SSE 对话开始: sessionId={}", sessionId);

                Flux<NodeOutput> flux = supervisorAgent.execute(context);

                flux.subscribe(
                        nodeOutput -> {
                            if (!(nodeOutput instanceof StreamingOutput<?> streaming)) return;
                            if (!(streaming.message() instanceof AssistantMessage msg)) return;
                            String text = msg.getText();
                            if (text == null || text.isBlank()) return;
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("token")
                                        .data(Map.of("content", text)));
                            } catch (IOException e) {
                                log.error("发送 token 事件失败", e);
                                emitter.completeWithError(e);
                            }
                        },
                        error -> {
                            log.error("SSE 对话异常: sessionId={}", sessionId, error);
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data(Map.of("error", String.valueOf(error.getMessage()))));
                            } catch (IOException e) {
                                log.error("发送错误事件失败", e);
                            }
                            emitter.completeWithError(error);
                        },
                        () -> {
                            log.info("SSE 对话完成: sessionId={}", sessionId);
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("done")
                                        .data(Map.of("sessionId", sessionId)));
                            } catch (IOException e) {
                                log.error("发送 done 事件失败", e);
                            }
                            emitter.complete();
                        }
                );
            } catch (Exception e) {
                log.error("启动 SSE 对话失败: sessionId={}", sessionId, e);
                emitter.completeWithError(e);
            }
        });

        emitter.onTimeout(() -> {
            log.warn("SSE 超时: sessionId={}", sessionId);
            emitter.complete();
        });
        emitter.onError(e -> log.error("SSE 连接错误: sessionId={}", sessionId, e));

        return emitter;
    }
}
