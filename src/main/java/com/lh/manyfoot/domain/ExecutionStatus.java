package com.lh.manyfoot.domain;

import lombok.Getter;

/**
 * 代码执行状态枚举
 *
 * @author airx
 */
@Getter
public enum ExecutionStatus {

    /**
     * 等待执行
     */
    PENDING("pending", "等待执行"),

    /**
     * 执行中
     */
    RUNNING("running", "执行中"),

    /**
     * 执行成功
     */
    SUCCESS("success", "执行成功"),

    /**
     * 执行失败
     */
    FAILED("failed", "执行失败"),

    /**
     * 执行超时
     */
    TIMEOUT("timeout", "执行超时"),

    /**
     * 已取消
     */
    CANCELLED("cancelled", "已取消");

    private final String code;
    private final String description;

    ExecutionStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
