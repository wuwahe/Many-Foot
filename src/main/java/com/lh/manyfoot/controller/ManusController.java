package com.lh.manyfoot.controller;

import cn.hutool.json.JSONUtil;
import com.lh.manyfoot.async.AsyncTaskManager;
import com.lh.manyfoot.async.domain.ManusTask;
import com.lh.manyfoot.async.domain.ManusTaskRequest;
import com.lh.manyfoot.async.domain.TaskResumeRequest;
import com.lh.manyfoot.async.domain.TaskResumeResult;
import com.lh.manyfoot.event.domain.ManusEvent;
import com.lh.manyfoot.event.service.EventStreamService;
import com.lh.manyfoot.orchestrator.ManusOrchestrator;
import com.lh.manyfoot.orchestrator.domain.ManusRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manus 执行控制器
 * 提供 SSE 流式执行、异步任务、断线重连等接口
 *
 * @author airx
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/manus")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "manus.sandbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ManusController {

    private final ManusOrchestrator orchestrator;
    private final AsyncTaskManager asyncTaskManager;
    private final EventStreamService eventStreamService;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * 流式执行 Manus Agent Loop (SSE)
     *
     * @param request 执行请求
     * @return SSE 事件流
     */
    @PostMapping(value = "/execute/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter executeStream(@Valid @RequestBody ManusRequest request) {
        log.info("接收到 Manus 流式执行请求: query={}",
            request.getQuery().substring(0, Math.min(50, request.getQuery().length())));

        // 创建 SSE 发射器，超时时间 10 分钟
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);

        executorService.execute(() -> {
            try {
                Flux<ManusEvent> eventFlux = orchestrator.execute(request);

                eventFlux.subscribe(
                    event -> {
                        try {
                            emitter.send(SseEmitter.event()
                                .name(event.getEventType().getCode())
                                .data(JSONUtil.toJsonStr(event)));
                        } catch (IOException e) {
                            log.error("发送SSE事件失败", e);
                            emitter.completeWithError(e);
                        }
                    },
                    error -> {
                        log.error("Manus执行错误", error);
                        try {
                            emitter.send(SseEmitter.event()
                                .name("error")
                                .data(JSONUtil.toJsonStr(ManusEvent.error(
                                    request.getSessionId(), error.getMessage()))));
                        } catch (IOException e) {
                            log.error("发送错误事件失败", e);
                        }
                        emitter.completeWithError(error);
                    },
                    () -> {
                        log.info("Manus执行完成");
                        emitter.complete();
                    }
                );
            } catch (Exception e) {
                log.error("启动Manus执行失败", e);
                emitter.completeWithError(e);
            }
        });

        // 设置超时和错误处理
        emitter.onTimeout(() -> {
            log.warn("SSE连接超时");
            emitter.complete();
        });

        emitter.onError(e -> {
            log.error("SSE连接错误", e);
        });

        return emitter;
    }

    /**
     * 提交异步任务
     *
     * @param request 任务请求
     * @return 任务ID
     */
    @PostMapping("/task/submit")
    public R<String> submitTask(@Valid @RequestBody ManusTaskRequest request) {
        log.info("提交异步任务: query={}",
            request.getQuery().substring(0, Math.min(50, request.getQuery().length())));

        String taskId = asyncTaskManager.submitTask(request);
        return R.ok(taskId, "任务已提交");
    }

    /**
     * 获取任务状态
     *
     * @param taskId 任务ID
     * @return 任务信息
     */
    @GetMapping("/task/{taskId}/status")
    public R<ManusTask> getTaskStatus(@PathVariable String taskId) {
        ManusTask task = asyncTaskManager.getTaskStatus(taskId);
        if (task == null) {
            return R.fail("任务不存在");
        }
        return R.ok(task);
    }

    /**
     * 取消任务
     *
     * @param taskId 任务ID
     * @return 是否成功
     */
    @PostMapping("/task/{taskId}/cancel")
    public R<Boolean> cancelTask(@PathVariable String taskId) {
        return R.ok(asyncTaskManager.cancelTask(taskId));
    }

    /**
     * 恢复任务连接 (断线重连)
     *
     * @param request 恢复请求
     * @return 恢复结果
     */
    @PostMapping("/task/resume")
    public R<TaskResumeResult> resumeTask(@Valid @RequestBody TaskResumeRequest request) {
        log.info("恢复任务连接: sessionId={}, lastSequence={}",
            request.getSessionId(), request.getLastEventSequence());

        TaskResumeResult result = asyncTaskManager.resumeTask(
            request.getSessionId(), request.getLastEventSequence());

        if (!Boolean.TRUE.equals(result.getFound())) {
            return R.fail(result.getErrorMessage());
        }
        return R.ok(result);
    }

    /**
     * 获取历史事件
     *
     * @param sessionId     会话ID
     * @param afterSequence 指定序号之后的事件 (可选)
     * @return 事件列表
     */
    @GetMapping("/events/{sessionId}")
    public R<List<ManusEvent>> getEvents(
        @PathVariable String sessionId,
        @RequestParam(required = false) Long afterSequence
    ) {
        List<ManusEvent> events;
        if (afterSequence != null) {
            events = eventStreamService.getEventsSince(sessionId, afterSequence);
        } else {
            events = eventStreamService.getEvents(sessionId);
        }
        return R.ok(events);
    }

    /**
     * 获取最近的事件
     *
     * @param sessionId 会话ID
     * @param count     数量
     * @return 事件列表
     */
    @GetMapping("/events/{sessionId}/recent")
    public R<List<ManusEvent>> getRecentEvents(
        @PathVariable String sessionId,
        @RequestParam(defaultValue = "20") Integer count
    ) {
        List<ManusEvent> events = eventStreamService.getRecentEvents(sessionId, count);
        return R.ok(events);
    }

    /**
     * 检查会话是否存在
     *
     * @param sessionId 会话ID
     * @return 是否存在
     */
    @GetMapping("/session/{sessionId}/exists")
    public R<Boolean> sessionExists(@PathVariable String sessionId) {
        boolean exists = eventStreamService.sessionExists(sessionId);
        return R.ok(exists);
    }
}
