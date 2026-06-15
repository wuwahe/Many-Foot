package com.lh.manyfoot.controller;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.lh.manyfoot.agent.context.AgentAttachment;
import com.lh.manyfoot.agent.context.AgentContext;
import com.lh.manyfoot.agent.context.SessionContextHolder;
import com.lh.manyfoot.agent.impl.SupervisorAgent;
import com.lh.manyfoot.agent.stream.ConversationEventStreamFactory;
import com.lh.manyfoot.agent.stream.event.Complete;
import com.lh.manyfoot.agent.stream.event.ConversationEvent;
import com.lh.manyfoot.agent.stream.event.Failure;
import com.lh.manyfoot.agent.stream.event.NarrationDelta;
import com.lh.manyfoot.agent.stream.event.PhaseHint;
import com.lh.manyfoot.dto.ChatRequest;
import com.lh.manyfoot.service.SandboxContainerManager;
import com.lh.manyfoot.service.file.FileStorageService;
import com.lh.manyfoot.service.file.UploadedFileInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
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
    private final ObjectProvider<SandboxContainerManager> sandboxContainerManagerProvider;
    private final ConversationEventStreamFactory eventStreamFactory;
    private final FileStorageService fileStorageService;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);

        String sessionId = request.getSessionId() != null
                ? request.getSessionId()
                : UUID.randomUUID().toString();

        executor.execute(() -> {
            try {
                // 设置会话ID到上下文，供子代理继承
                SessionContextHolder.setSessionId(sessionId);

                List<AgentAttachment> attachments = buildAttachments(sessionId, request.getFilePaths());
                AgentContext context = AgentContext.builder()
                        .sessionId(sessionId)
                        .query(request.getMessage())
                        .attachments(attachments)
                        .build();
                SessionContextHolder.setAgentContext(context);
                if (!attachments.isEmpty()) {
                    context.setAttribute("filePaths", attachments.stream()
                            .map(AgentAttachment::getSandboxPath)
                            .toList());
                    log.info("SSE 对话包含附件: sessionId={}, imageCount={}, fileCount={}",
                            sessionId, context.getImageAttachments().size(), context.getFileAttachments().size());
                }

                log.info("SSE 对话开始: sessionId={}", sessionId);

                Flux<NodeOutput> raw = supervisorAgent.execute(context);
                Flux<ConversationEvent> events = eventStreamFactory.create(raw, sessionId)
                        .doFinally(signalType -> {
                            log.debug("SSE 对话流结束，清理会话上下文: sessionId={}, signal={}", sessionId, signalType);
                            SessionContextHolder.clear();
                        });

                events.subscribe(
                        ev -> sendEvent(emitter, ev, sessionId),
                        err -> sendError(emitter, sessionId, err),    // 兜底；正常错误已变成 Failure 事件
                        () -> log.info("SSE 对话完成: sessionId={}", sessionId)
                );
            } catch (Exception e) {
                log.error("启动 SSE 对话失败: sessionId={}", sessionId, e);
                SessionContextHolder.clear();
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

    @PostMapping("/upload")
    public Map<String, String> upload(@RequestParam String sessionId,
                                       @RequestParam MultipartFile file) {
        UploadedFileInfo info = fileStorageService.upload(sessionId, file);
        return Map.of(
                "path", info.getPath(),
                "mimeType", info.getMimeType(),
                "type", info.getType()
        );
    }

    private List<AgentAttachment> buildAttachments(String sessionId, List<String> filePaths) {
        if (filePaths == null || filePaths.isEmpty()) {
            return List.of();
        }
        return filePaths.stream()
                .map(sandboxPath -> buildAttachment(sessionId, sandboxPath))
                .toList();
    }

    private AgentAttachment buildAttachment(String sessionId, String sandboxPath) {
        String filename = Path.of(sandboxPath).getFileName().toString();
        SandboxContainerManager sandboxContainerManager = sandboxContainerManagerProvider.getIfAvailable();
        String hostPath = resolveHostPath(sessionId, sandboxPath, sandboxContainerManager);
        String mimeType = detectMimeType(filename, null, sandboxPath);
        return AgentAttachment.builder()
                .sandboxPath(sandboxPath)
                .hostPath(hostPath)
                .filename(filename)
                .mimeType(mimeType)
                .image(isImage(mimeType))
                .build();
    }

    private String detectMimeType(String filename, String declaredContentType, String path) {
        if (declaredContentType != null && !declaredContentType.isBlank()) {
            return declaredContentType;
        }
        try {
            String detected = Files.probeContentType(Path.of(filename != null ? filename : path));
            if (detected != null && !detected.isBlank()) {
                return detected;
            }
        } catch (IOException e) {
            log.debug("探测文件 MIME 类型失败: path={}", path, e);
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private String resolveHostPath(String sessionId, String sandboxPath, SandboxContainerManager sandboxContainerManager) {
        if (sandboxContainerManager == null) {
            return null;
        }
        try {
            return sandboxContainerManager.toLocalAttachmentPath(sessionId, sandboxPath);
        } catch (IllegalArgumentException e) {
            log.warn("忽略非法附件路径: sessionId={}, sandboxPath={}, error={}", sessionId, sandboxPath, e.getMessage());
            return null;
        }
    }

    private boolean isImage(String mimeType) {
        return mimeType != null && mimeType.toLowerCase().startsWith("image/");
    }

    private void sendEvent(SseEmitter emitter, ConversationEvent ev, String sessionId) {
        try {
            if (ev instanceof NarrationDelta nd) {
                emitter.send(SseEmitter.event().name("message").data(Map.of("text", nd.text())));
            } else if (ev instanceof PhaseHint ph) {
                emitter.send(SseEmitter.event().name("phase")
                        .data(Map.of("phase", ph.phase().name().toLowerCase(Locale.ROOT))));
            } else if (ev instanceof Complete c) {
                emitter.send(SseEmitter.event().name("done").data(Map.of("sessionId", c.sessionId())));
                emitter.complete();
            } else if (ev instanceof Failure f) {
                log.error("SSE 对话异常: sessionId={}", sessionId, f.cause());
                emitter.send(SseEmitter.event().name("error").data(Map.of("error", f.userMessage())));
                emitter.complete();   // 不 completeWithError；事件已经送达
            }
        } catch (IOException e) {
            log.error("发送 SSE 事件失败: sessionId={}, type={}", sessionId, ev.getClass().getSimpleName(), e);
            emitter.completeWithError(e);
        }
    }

    private void sendError(SseEmitter emitter, String sessionId, Throwable err) {
        // 兜底：正常情况下 translator 已经把异常包成了 Failure 事件；这里只兜住订阅链异常
        log.error("SSE 订阅链异常: sessionId={}", sessionId, err);
        try {
            emitter.send(SseEmitter.event().name("error").data(Map.of("error", String.valueOf(err.getMessage()))));
        } catch (IOException e) {
            log.error("发送 error 事件失败", e);
        }
        emitter.completeWithError(err);
    }
}
