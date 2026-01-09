package com.lh.manyfoot.orchestrator.domain;

/**
 * 子任务类型枚举
 *
 * @author airx
 */
public enum SubtaskType {

    /**
     * 代码执行
     */
    CODE_EXECUTION,

    /**
     * 文件操作
     */
    FILE_OPERATION,

    /**
     * 数据处理
     */
    DATA_PROCESSING,

    /**
     * API调用
     */
    API_CALL,

    /**
     * 结果验证
     */
    VALIDATION
}