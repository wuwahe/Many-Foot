package com.lh.manyfoot.orchestrator.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 子任务
 *
 * @author airx
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subtask {

    /**
     * 子任务ID
     */
    private Integer id;

    /**
     * 子任务名称
     */
    private String name;

    /**
     * 子任务描述
     */
    private String description;

    /**
     * 子任务类型
     */
    private SubtaskType type;

    /**
     * 依赖的子任务ID列表
     */
    private List<Integer> dependencies;

    /**
     * 是否可以并行执行
     */
    private Boolean parallel;

    /**
     * 执行状态
     */
    @Builder.Default
    private SubtaskStatus status = SubtaskStatus.PENDING;

    /**
     * 执行结果
     */
    private String result;

    /**
     * 子任务状态枚举
     */
    public enum SubtaskStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        SKIPPED
    }
}
