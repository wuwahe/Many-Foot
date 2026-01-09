package com.lh.manyfoot.codeact.domain;

import com.lh.manyfoot.domain.ExecutionResult;
import com.lh.manyfoot.domain.ExecutionStatus;
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
public class CodeExecutionResult implements Serializable {

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
     * 代码类型
     */
    private CodeType codeType;

    /**
     * 执行的代码
     */
    private String code;

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
     * 从 ExecutionResult 转换
     */
    public static CodeExecutionResult from(ExecutionResult result, String sessionId, CodeType codeType, String code) {
        return CodeExecutionResult.builder()
            .executionId(result.getExecutionId())
            .sessionId(sessionId)
            .codeType(codeType)
            .code(code)
            .exitCode(result.getExitCode())
            .stdout(result.getStdout())
            .stderr(result.getStderr())
            .executionTime(result.getExecutionTime())
            .timeout(result.getTimeout())
            .status(result.getStatus())
            .errorMessage(result.getErrorMessage())
            .build();
    }

    /**
     * 获取格式化的输出
     */
    public String getFormattedOutput() {
        StringBuilder sb = new StringBuilder();

        if (stdout != null && !stdout.isEmpty()) {
            sb.append(stdout);
        }

        if (stderr != null && !stderr.isEmpty()) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append("[STDERR]\n").append(stderr);
        }

        return sb.toString();
    }
}
