package com.lh.manyfoot.event.domain;

import lombok.Getter;

/**
 * Manus 事件类型枚举
 *
 * @author airx
 */
@Getter
public enum ManusEventType {

    // ==================== Agent Loop 相关 ====================

    /**
     * 循环开始
     */
    LOOP_START("loop_start", "循环开始"),

    /**
     * 循环迭代
     */
    LOOP_ITERATION("loop_iteration", "循环迭代"),

    /**
     * 循环完成
     */
    LOOP_COMPLETE("loop_complete", "循环完成"),

    // ==================== 四阶段事件 ====================

    /**
     * 分析阶段
     */
    ANALYZE("analyze", "分析阶段"),

    /**
     * 规划阶段
     */
    PLAN("plan", "规划阶段"),

    /**
     * 执行阶段
     */
    EXECUTE("execute", "执行阶段"),

    /**
     * 观察阶段
     */
    OBSERVE("observe", "观察阶段"),

    // ==================== 代码执行相关 ====================

    /**
     * 代码执行开始
     */
    CODE_EXECUTION_START("code_execution_start", "代码执行开始"),

    /**
     * 代码执行输出
     */
    CODE_EXECUTION_OUTPUT("code_execution_output", "代码执行输出"),

    /**
     * 代码执行完成
     */
    CODE_EXECUTION_COMPLETE("code_execution_complete", "代码执行完成"),

    /**
     * 代码执行错误
     */
    CODE_EXECUTION_ERROR("code_execution_error", "代码执行错误"),

    // ==================== 容器相关 ====================

    /**
     * 容器创建
     */
    CONTAINER_CREATED("container_created", "容器创建"),

    /**
     * 容器启动
     */
    CONTAINER_STARTED("container_started", "容器启动"),

    /**
     * 容器停止
     */
    CONTAINER_STOPPED("container_stopped", "容器停止"),

    // ==================== 任务相关 ====================

    /**
     * 任务创建
     */
    TASK_CREATED("task_created", "任务创建"),

    /**
     * 任务进度
     */
    TASK_PROGRESS("task_progress", "任务进度"),

    /**
     * 任务完成
     */
    TASK_COMPLETE("task_complete", "任务完成"),

    /**
     * 任务失败
     */
    TASK_FAILED("task_failed", "任务失败"),

    // ==================== Agent 思考过程 ====================

    /**
     * 思考中
     */
    THINKING("thinking", "思考中"),

    /**
     * Agent 输出
     */
    AGENT_OUTPUT("agent_output", "Agent输出"),

    /**
     * 工具调用
     */
    TOOL_CALL("tool_call", "工具调用"),

    /**
     * 工具结果
     */
    TOOL_RESULT("tool_result", "工具结果"),

    // ==================== 错误相关 ====================

    /**
     * 错误
     */
    ERROR("error", "错误"),

    /**
     * 警告
     */
    WARNING("warning", "警告");

    private final String code;
    private final String description;

    ManusEventType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据代码获取枚举
     */
    public static ManusEventType fromCode(String code) {
        for (ManusEventType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
