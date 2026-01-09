package com.lh.manyfoot.async.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Manus 任务实体
 *
 * @author airx
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManusTask implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户查询
     */
    private String query;

    /**
     * 任务状态
     */
    private TaskStatus status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 完成时间
     */
    private LocalDateTime completeTime;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 当前迭代次数
     */
    private Integer currentIteration;

    /**
     * 最大迭代次数
     */
    private Integer maxIterations;

    /**
     * 模型ID
     */
    private Long modelId;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 最终结果摘要
     */
    private String finalSummary;
}
