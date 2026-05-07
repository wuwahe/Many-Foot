package com.lh.manyfoot.models.registry;

import lombok.Getter;

/**
 * 模型角色枚举
 * 定义 Supervisor 专业智能体中不同角色使用的模型类型
 */
@Getter
public enum ModelRole {

    /**
     * 规划与路由智能体
     */
    PLANNER_ROUTER("planner-router", "规划与路由智能体"),

    /**
     * 检索与证据智能体
     */
    RESEARCH_RETRIEVAL("research-retrieval", "检索与证据智能体"),

    /**
     * 领域专家智能体
     */
    DOMAIN_SPECIALIST("domain-specialist", "领域专家智能体"),

    /**
     * 工具与事务执行智能体
     */
    TOOL_ACTION_EXECUTOR("tool-action-executor", "工具与事务执行智能体"),

    /**
     * 评审与验证智能体
     */
    CRITIC_VERIFIER("critic-verifier", "评审与验证智能体"),

    /**
     * 普通对话智能体
     */
    CHAT("chat", "多模态文档分析助手"),

    /**
     * Supervisor 编排智能体
     */
    SUPERVISOR("supervisor", "Supervisor 编排智能体");

    private final String code;
    private final String description;

    ModelRole(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
