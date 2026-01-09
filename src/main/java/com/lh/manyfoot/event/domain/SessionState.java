package com.lh.manyfoot.event.domain;

import com.lh.manyfoot.async.domain.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 会话状态实体
 * 用于保存和恢复 Agent Loop 的执行状态
 *
 * @author airx
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionState implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 当前迭代次数
     */
    private Integer currentIteration;

    /**
     * 最大迭代次数
     */
    private Integer maxIterations;

    /**
     * 当前阶段
     */
    private LoopPhase currentPhase;

    /**
     * 任务状态
     */
    private TaskStatus taskStatus;

    /**
     * 原始查询
     */
    private String originalQuery;

    /**
     * 当前任务计划
     */
    private String currentPlan;

    /**
     * 累积的观察结果
     */
    @Builder.Default
    private List<String> observations = new ArrayList<>();

    /**
     * 容器ID
     */
    private String containerId;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 最后活动时间
     */
    private LocalDateTime lastActiveTime;

    /**
     * 最后处理的事件序号
     */
    private Long lastEventSequence;

    /**
     * 是否已完成
     */
    private Boolean completed;

    /**
     * 最终结果摘要
     */
    private String finalSummary;

    /**
     * 添加观察结果
     */
    public void addObservation(String observation) {
        if (this.observations == null) {
            this.observations = new ArrayList<>();
        }
        this.observations.add(observation);
    }

    /**
     * 更新活动时间
     */
    public void touch() {
        this.lastActiveTime = LocalDateTime.now();
    }

    /**
     * 创建初始状态
     */
    public static SessionState createInitial(String sessionId, String query, int maxIterations, Long tenantId, Long userId) {
        return SessionState.builder()
            .sessionId(sessionId)
            .originalQuery(query)
            .currentIteration(0)
            .maxIterations(maxIterations)
            .currentPhase(LoopPhase.ANALYZE)
            .taskStatus(TaskStatus.PENDING)
            .observations(new ArrayList<>())
            .tenantId(tenantId)
            .userId(userId)
            .createTime(LocalDateTime.now())
            .lastActiveTime(LocalDateTime.now())
            .lastEventSequence(0L)
            .completed(false)
            .build();
    }
}
