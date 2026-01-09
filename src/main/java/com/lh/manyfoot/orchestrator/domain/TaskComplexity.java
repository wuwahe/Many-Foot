package com.lh.manyfoot.orchestrator.domain;

/**
 * 任务复杂度枚举
 *
 * @author airx
 */
public enum TaskComplexity {

    /**
     * 简单任务：单一明确动作，无需外部依赖，可一步完成
     */
    SIMPLE,

    /**
     * 中等任务：需要2-3个有序步骤，但逻辑清晰
     */
    MEDIUM,

    /**
     * 复杂任务：需要多个子任务协作、有并行可能、或需要动态决策
     */
    COMPLEX
}