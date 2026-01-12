package com.lh.manyfoot.models.registry;

import lombok.Getter;

/**
 * 模型角色枚举
 * 定义智能体中不同角色使用的模型类型
 */
@Getter
public enum ModelRole {

    /**
     * 分析角色 - 用于复杂推理和规划，通常使用较强的模型
     */
    ANALYZE("analyze", "分析推理"),

    /**
     * 执行角色 - 用于执行具体任务，平衡能力和成本
     */
    EXECUTE("execute", "任务执行"),

    /**
     * 观察角色 - 用于监控和简单判断，使用轻量级模型
     */
    OBSERVE("observe", "状态观察");

    private final String code;
    private final String description;

    ModelRole(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
