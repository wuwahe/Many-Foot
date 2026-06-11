package com.lh.manyfoot.agent.tool.sandbox.format;

import cn.hutool.core.util.StrUtil;
import com.lh.manyfoot.agent.tool.sandbox.domain.SandboxExecutionResult;

/**
 * 沙箱执行结果格式化器
 *
 * <p>
 * 将 {@link SandboxExecutionResult} 转换为适合 Agent 阅读的自然语言文本，
 * 包含执行状态、退出码、耗时、标准输出/错误及超时提示。
 *
 * @author airx
 */
public final class ExecutionResultFormatter {

    private static final int MAX_STDOUT_LENGTH = 5000;
    private static final int MAX_STDERR_LENGTH = 2000;

    private ExecutionResultFormatter() {
    }

    public static String format(SandboxExecutionResult result) {
        StringBuilder sb = new StringBuilder();

        sb.append("执行状态: ").append(result.isSuccess() ? "成功" : "失败").append("\n");
        sb.append("退出码: ").append(result.getExitCode()).append("\n");
        sb.append("执行耗时: ").append(result.getExecutionTime()).append("ms\n");

        if (StrUtil.isNotBlank(result.getStdout())) {
            sb.append("\n--- 输出 ---\n");
            sb.append(truncate(result.getStdout(), MAX_STDOUT_LENGTH));
        }

        if (StrUtil.isNotBlank(result.getStderr())) {
            sb.append("\n--- 错误 ---\n");
            sb.append(truncate(result.getStderr(), MAX_STDERR_LENGTH));
        }

        if (Boolean.TRUE.equals(result.getTimeout())) {
            sb.append("\n[警告] 执行超时，结果可能不完整");
        }

        return sb.toString();
    }

    private static String truncate(String output, int maxLength) {
        if (output == null || output.length() <= maxLength) {
            return output;
        }
        return output.substring(0, maxLength)
            + "\n... [输出已截断，共 " + output.length() + " 字符]";
    }
}
