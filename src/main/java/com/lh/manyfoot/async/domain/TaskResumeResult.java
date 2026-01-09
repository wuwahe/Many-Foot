package com.lh.manyfoot.async.domain;

import com.lh.manyfoot.event.domain.ManusEvent;
import com.lh.manyfoot.event.domain.SessionState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 任务恢复结果
 *
 * @author airx
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResumeResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否找到会话
     */
    private Boolean found;

    /**
     * 会话状态
     */
    private SessionState sessionState;

    /**
     * 未接收的事件列表
     */
    @Builder.Default
    private List<ManusEvent> missedEvents = Collections.emptyList();

    /**
     * 任务是否还在运行
     */
    private Boolean isRunning;

    /**
     * 当前事件序号
     */
    private Long currentSequence;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建未找到结果
     */
    public static TaskResumeResult notFound() {
        return TaskResumeResult.builder()
            .found(false)
            .isRunning(false)
            .missedEvents(Collections.emptyList())
            .errorMessage("会话不存在或已过期")
            .build();
    }

    /**
     * 创建成功结果
     */
    public static TaskResumeResult success(SessionState state, List<ManusEvent> missedEvents,
                                           boolean isRunning, long currentSequence) {
        return TaskResumeResult.builder()
            .found(true)
            .sessionState(state)
            .missedEvents(missedEvents)
            .isRunning(isRunning)
            .currentSequence(currentSequence)
            .build();
    }
}
