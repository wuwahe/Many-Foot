package com.lh.manyfoot.tools;

import cn.hutool.core.util.StrUtil;
import com.lh.manyfoot.codeact.CodeActEngine;
import com.lh.manyfoot.codeact.domain.CodeExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CodeAct 工具类
 * 提供给 Agent 调用的代码执行工具
 *
 * @author airx
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "many-foot.sandbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CodeActTool {

    private final CodeActEngine codeActEngine;

    /**
     * 执行 Python 代码
     */
    @Tool(description = "在安全的沙箱环境中执行Python代码。支持数据处理、计算、文件操作、API调用等任务。代码执行在隔离的Docker容器中，确保安全性。")
    public String executePython(
        @ToolParam(description = "要执行的Python代码。支持多行代码，可以导入常用库如numpy、pandas、requests等。") String code,
        @ToolParam(description = "会话ID，用于标识执行环境") String sessionId
    ) {
        log.info("Agent调用执行Python代码: sessionId={}, code length={}", sessionId, code.length());

        try {
            CodeExecutionResult result = codeActEngine.executePython(sessionId, code, null, null);
            return formatExecutionResult(result);
        } catch (Exception e) {
            log.error("执行Python代码失败", e);
            return "执行失败: " + e.getMessage();
        }
    }

    /**
     * 执行 Shell 命令
     */
    @Tool(description = "在沙箱环境中执行Shell命令。用于系统操作、文件管理、包安装、进程管理等任务。")
    public String executeShell(
        @ToolParam(description = "要执行的Shell命令。支持管道、重定向等Shell特性。") String command,
        @ToolParam(description = "会话ID，用于标识执行环境") String sessionId
    ) {
        log.info("Agent调用执行Shell命令: sessionId={}, command={}", sessionId,
            command.substring(0, Math.min(100, command.length())));

        try {
            CodeExecutionResult result = codeActEngine.executeShell(sessionId, command, null, null);
            return formatExecutionResult(result);
        } catch (Exception e) {
            log.error("执行Shell命令失败", e);
            return "执行失败: " + e.getMessage();
        }
    }

    /**
     * 读取沙箱中的文件
     */
    @Tool(description = "读取沙箱环境中的文件内容。可以读取代码执行产生的输出文件、日志文件等。")
    public String readSandboxFile(
        @ToolParam(description = "文件路径，相对于工作目录。例如: /output.txt, /data/result.json") String filePath,
        @ToolParam(description = "会话ID") String sessionId
    ) {
        log.info("Agent读取沙箱文件: sessionId={}, path={}", sessionId, filePath);

        try {
            String content = codeActEngine.readFile(sessionId, filePath);
            return "文件内容:\n" + content;
        } catch (Exception e) {
            log.error("读取文件失败", e);
            return "读取失败: " + e.getMessage();
        }
    }

    /**
     * 写入文件到沙箱
     */
    @Tool(description = "将内容写入沙箱环境中的文件。可以创建数据文件、配置文件、脚本文件等。")
    public String writeSandboxFile(
        @ToolParam(description = "文件路径，相对于工作目录。例如: /data.csv, /config.json") String filePath,
        @ToolParam(description = "要写入的文件内容") String content,
        @ToolParam(description = "会话ID") String sessionId
    ) {
        log.info("Agent写入沙箱文件: sessionId={}, path={}, content length={}",
            sessionId, filePath, content.length());

        try {
            codeActEngine.writeFile(sessionId, filePath, content);
            return "文件写入成功: " + filePath;
        } catch (Exception e) {
            log.error("写入文件失败", e);
            return "写入失败: " + e.getMessage();
        }
    }

    /**
     * 列出沙箱目录内容
     */
    @Tool(description = "列出沙箱环境中指定目录的文件和子目录。")
    public String listSandboxDirectory(
        @ToolParam(description = "目录路径，相对于工作目录。使用 / 表示根目录。") String dirPath,
        @ToolParam(description = "会话ID") String sessionId
    ) {
        log.info("Agent列出沙箱目录: sessionId={}, path={}", sessionId, dirPath);

        try {
            List<String> entries = codeActEngine.listDirectory(sessionId, dirPath);
            if (entries.isEmpty()) {
                return "目录为空";
            }
            return "目录内容:\n" + String.join("\n", entries);
        } catch (Exception e) {
            log.error("列出目录失败", e);
            return "列出失败: " + e.getMessage();
        }
    }

    /**
     * 安装 Python 包
     */
    @Tool(description = "在沙箱环境中安装Python包。使用pip进行安装。")
    public String installPythonPackage(
        @ToolParam(description = "要安装的Python包名。例如: pandas, numpy==1.24.0, requests>=2.28") String packageName,
        @ToolParam(description = "会话ID") String sessionId
    ) {
        log.info("Agent安装Python包: sessionId={}, package={}", sessionId, packageName);

        try {
            CodeExecutionResult result = codeActEngine.installPythonPackage(sessionId, packageName, null, null);
            return formatExecutionResult(result);
        } catch (Exception e) {
            log.error("安装Python包失败", e);
            return "安装失败: " + e.getMessage();
        }
    }

    /**
     * 格式化执行结果
     */
    private String formatExecutionResult(CodeExecutionResult result) {
        StringBuilder sb = new StringBuilder();

        // 执行状态
        sb.append("执行状态: ").append(result.isSuccess() ? "成功" : "失败").append("\n");
        sb.append("退出码: ").append(result.getExitCode()).append("\n");
        sb.append("执行耗时: ").append(result.getExecutionTime()).append("ms\n");

        // 标准输出
        if (StrUtil.isNotBlank(result.getStdout())) {
            sb.append("\n--- 输出 ---\n");
            sb.append(truncateOutput(result.getStdout(), 5000));
        }

        // 标准错误
        if (StrUtil.isNotBlank(result.getStderr())) {
            sb.append("\n--- 错误 ---\n");
            sb.append(truncateOutput(result.getStderr(), 2000));
        }

        // 超时提示
        if (Boolean.TRUE.equals(result.getTimeout())) {
            sb.append("\n[警告] 执行超时，结果可能不完整");
        }

        return sb.toString();
    }

    /**
     * 截断过长的输出
     */
    private String truncateOutput(String output, int maxLength) {
        if (output == null || output.length() <= maxLength) {
            return output;
        }
        return output.substring(0, maxLength) + "\n... [输出已截断，共 " + output.length() + " 字符]";
    }
}
