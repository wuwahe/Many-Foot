package com.lh.manyfoot.async;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.lh.manyfoot.async.domain.ManusTask;
import com.lh.manyfoot.async.domain.ManusTaskRequest;
import com.lh.manyfoot.async.domain.TaskResumeResult;
import com.lh.manyfoot.async.domain.TaskStatus;
import com.lh.manyfoot.event.domain.ManusEvent;
import com.lh.manyfoot.event.domain.ManusEventType;
import com.lh.manyfoot.event.domain.SessionState;
import com.lh.manyfoot.event.service.EventStreamService;
import com.lh.manyfoot.orchestrator.ManusOrchestrator;
import com.lh.manyfoot.orchestrator.domain.ManusRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.redis.utils.RedisUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * 异步任务管理器
 * 支持任务提交、状态查询、断线重连
 *
 * @author airx
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "manus.sandbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AsyncTaskManager {

    private final EventStreamService eventStreamService;
    private final ManusOrchestrator orchestrator;

    // Redis 键前缀
    private static final String TASK_KEY_PREFIX = "manus:task:";
    private static final Duration TASK_RETENTION = Duration.ofDays(7);

    // 异步执行线程池
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    // 运行中的任务
    private final ConcurrentHashMap<String, CompletableFuture<Void>> runningTasks = new ConcurrentHashMap<>();

    /**
     * 提交异步任务
     *
     * @param request 任务请求
     * @return 任务ID
     */
    public String submitTask(ManusTaskRequest request) {
        String taskId = IdUtil.fastSimpleUUID();
        String sessionId = StrUtil.isBlank(request.getSessionId())
            ? IdUtil.fastSimpleUUID()
            : request.getSessionId();

        // 创建任务实体
        ManusTask task = ManusTask.builder()
            .taskId(taskId)
            .sessionId(sessionId)
            .query(request.getQuery())
            .status(TaskStatus.PENDING)
            .createTime(LocalDateTime.now())
            .maxIterations(request.getMaxIterations())
            .modelId(request.getModelId())
            .tenantId(request.getTenantId())
            .userId(request.getUserId())
            .build();

        // 保存任务状态
        saveTask(task);

        // 发布任务创建事件
        eventStreamService.appendEvent(sessionId, ManusEvent.builder()
            .eventType(ManusEventType.TASK_CREATED)
            .sessionId(sessionId)
            .content("任务已创建: " + taskId)
            .timestamp(System.currentTimeMillis())
            .build());

        // 异步执行任务
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            executeTask(task);
        }, executorService);

        runningTasks.put(taskId, future);

        // 任务完成后从运行列表移除
        future.whenComplete((result, error) -> {
            runningTasks.remove(taskId);
        });

        log.info("异步任务已提交: taskId={}, sessionId={}", taskId, sessionId);
        return taskId;
    }

    /**
     * 执行任务
     */
    private void executeTask(ManusTask task) {
        try {
            // 更新任务状态为运行中
            task.setStatus(TaskStatus.RUNNING);
            task.setStartTime(LocalDateTime.now());
            saveTask(task);

            // 构建请求
            ManusRequest request = ManusRequest.builder()
                .sessionId(task.getSessionId())
                .query(task.getQuery())
                .maxIterations(task.getMaxIterations())
                .modelId(task.getModelId())
                .tenantId(task.getTenantId())
                .userId(task.getUserId())
                .build();

            // 执行 Agent Loop
            orchestrator.execute(request)
                .doOnNext(event -> {
                    // 更新任务进度
                    if (event.getIteration() != null) {
                        task.setCurrentIteration(event.getIteration());
                        saveTask(task);
                    }
                })
                .doOnComplete(() -> {
                    // 任务完成
                    task.setStatus(TaskStatus.COMPLETED);
                    task.setCompleteTime(LocalDateTime.now());

                    // 获取最终总结
                    SessionState state = eventStreamService.restoreSessionState(task.getSessionId());
                    if (state != null) {
                        task.setFinalSummary(state.getFinalSummary());
                    }

                    saveTask(task);
                    log.info("任务执行完成: taskId={}", task.getTaskId());
                })
                .doOnError(error -> {
                    // 任务失败
                    task.setStatus(TaskStatus.FAILED);
                    task.setCompleteTime(LocalDateTime.now());
                    task.setErrorMessage(error.getMessage());
                    saveTask(task);
                    log.error("任务执行失败: taskId={}", task.getTaskId(), error);
                })
                .blockLast(); // 阻塞等待完成

        } catch (Exception e) {
            task.setStatus(TaskStatus.FAILED);
            task.setCompleteTime(LocalDateTime.now());
            task.setErrorMessage(e.getMessage());
            saveTask(task);
            log.error("任务执行异常: taskId={}", task.getTaskId(), e);
        }
    }

    /**
     * 获取任务状态
     */
    public ManusTask getTaskStatus(String taskId) {
        return RedisUtils.getCacheObject(TASK_KEY_PREFIX + taskId);
    }

    /**
     * 取消任务
     */
    public boolean cancelTask(String taskId) {
        ManusTask task = getTaskStatus(taskId);
        if (task == null) {
            return false;
        }

        // 取消运行中的任务
        CompletableFuture<Void> future = runningTasks.get(taskId);
        if (future != null && !future.isDone()) {
            future.cancel(true);
            runningTasks.remove(taskId);
        }

        // 更新任务状态
        task.setStatus(TaskStatus.CANCELLED);
        task.setCompleteTime(LocalDateTime.now());
        saveTask(task);

        log.info("任务已取消: taskId={}", taskId);
        return true;
    }

    /**
     * 恢复任务连接 (断线重连)
     *
     * @param sessionId         会话ID
     * @param lastEventSequence 最后接收的事件序号
     * @return 恢复结果
     */
    public TaskResumeResult resumeTask(String sessionId, Long lastEventSequence) {
        // 1. 恢复会话状态
        SessionState state = eventStreamService.restoreSessionState(sessionId);
        if (state == null) {
            return TaskResumeResult.notFound();
        }

        // 2. 获取未接收的事件
        long afterSequence = lastEventSequence != null ? lastEventSequence : 0L;
        List<ManusEvent> missedEvents = eventStreamService.getEventsSince(sessionId, afterSequence);

        // 3. 检查任务是否还在执行
        boolean isRunning = state.getTaskStatus() == TaskStatus.RUNNING;

        // 4. 获取当前序列号
        long currentSequence = eventStreamService.getCurrentSequence(sessionId);

        return TaskResumeResult.success(state, missedEvents, isRunning, currentSequence);
    }

    /**
     * 保存任务
     */
    private void saveTask(ManusTask task) {
        RedisUtils.setCacheObject(TASK_KEY_PREFIX + task.getTaskId(), task, TASK_RETENTION);
    }

    /**
     * 检查任务是否在运行
     */
    public boolean isTaskRunning(String taskId) {
        CompletableFuture<Void> future = runningTasks.get(taskId);
        return future != null && !future.isDone();
    }
}
