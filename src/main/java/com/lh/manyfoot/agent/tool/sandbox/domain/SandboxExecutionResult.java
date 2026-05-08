package com.lh.manyfoot.agent.tool.sandbox.domain;

import com.lh.manyfoot.domain.ExecutionResult;
import com.lh.manyfoot.domain.ExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 沙箱代码执行结果
 *
 * <p>
 * 聚合执行状态、标准输出/错误、退出码、耗时等关键信息。
 * 提供便捷判断（{@link #isSuccess()}）和格式化输出（{@link #getFormattedOutput()}）。
 *
 * @author airx
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SandboxExecutionResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String executionId;

    private String sessionId;

    private SandboxCodeType codeType;

    private String code;

    private Integer exitCode;

    private String stdout;

    private String stderr;

    private Long executionTime;

    private Boolean timeout;

    private ExecutionStatus status;

    private String errorMessage;

    public boolean isSuccess() {
        return exitCode != null && exitCode == 0 && !Boolean.TRUE.equals(timeout);
    }

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

    public static SandboxExecutionResult from(ExecutionResult result, String sessionId,
                                              SandboxCodeType codeType, String code) {
        return SandboxExecutionResult.builder()
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
}
