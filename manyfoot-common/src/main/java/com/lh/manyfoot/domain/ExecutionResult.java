package com.lh.manyfoot.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 代码执行结果
 *
 * @author airx
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 执行ID
     */
    private String executionId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 退出码
     */
    private Integer exitCode;

    /**
     * 标准输出
     */
    private String stdout;

    /**
     * 标准错误
     */
    private String stderr;

    /**
     * 执行时长(毫秒)
     */
    private Long executionTime;

    /**
     * 是否超时
     */
    private Boolean timeout;

    /**
     * 执行状态
     */
    private ExecutionStatus status;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 判断执行是否成功
     */
    public boolean isSuccess() {
        return exitCode != null && exitCode == 0 && !Boolean.TRUE.equals(timeout);
    }

    /**
     * 创建成功结果
     */
    public static ExecutionResult success(String executionId, String stdout, long executionTime) {
        return ExecutionResult.builder()
            .executionId(executionId)
            .exitCode(0)
            .stdout(stdout)
            .stderr("")
            .executionTime(executionTime)
            .timeout(false)
            .status(ExecutionStatus.SUCCESS)
            .build();
    }

    /**
     * 创建失败结果
     */
    public static ExecutionResult failure(String executionId, int exitCode, String stderr, long executionTime) {
        return ExecutionResult.builder()
            .executionId(executionId)
            .exitCode(exitCode)
            .stdout("")
            .stderr(stderr)
            .executionTime(executionTime)
            .timeout(false)
            .status(ExecutionStatus.FAILED)
            .build();
    }

    /**
     * 创建超时结果
     */
    public static ExecutionResult timeout(String executionId, long executionTime) {
        return ExecutionResult.builder()
            .executionId(executionId)
            .exitCode(-1)
            .stdout("")
            .stderr("执行超时")
            .executionTime(executionTime)
            .timeout(true)
            .status(ExecutionStatus.TIMEOUT)
            .errorMessage("执行超时")
            .build();
    }

    /**
     * 创建错误结果
     */
    public static ExecutionResult error(String executionId, String errorMessage) {
        return ExecutionResult.builder()
            .executionId(executionId)
            .exitCode(-1)
            .stdout("")
            .stderr(errorMessage)
            .timeout(false)
            .status(ExecutionStatus.FAILED)
            .errorMessage(errorMessage)
            .build();
    }
}
