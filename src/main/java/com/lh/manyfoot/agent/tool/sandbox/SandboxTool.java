package com.lh.manyfoot.agent.tool.sandbox;

import cn.hutool.core.util.StrUtil;
import com.lh.manyfoot.agent.context.AgentContext;
import com.lh.manyfoot.agent.tool.sandbox.domain.SandboxExecutionResult;
import com.lh.manyfoot.agent.tool.sandbox.engine.SandboxEngine;
import com.lh.manyfoot.agent.tool.sandbox.format.DocumentParser;
import com.lh.manyfoot.agent.tool.sandbox.format.ExecutionResultFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 沙箱工具
 *
 * <p>
 * 供 Agent 调用的沙箱代码执行与文件操作工具集。
 * 涵盖 Python/Shell 执行、文件读写、目录浏览、文档解析及包安装等能力。
 * 所有操作均在隔离的 Docker 容器中进行，通过 sessionId 实现会话隔离。
 *
 * @author airx
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "many-foot.sandbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SandboxTool {

    private static final long MAX_DOCUMENT_BYTES = 20L * 1024L * 1024L;

    private final SandboxEngine sandboxEngine;

    @Tool(description = "在安全的沙箱环境中执行Python代码。支持数据处理、计算、文件操作、API调用等任务。代码执行在隔离的Docker容器中，确保安全性。")
    public String executePython(
        @ToolParam(description = "要执行的Python代码。支持多行代码，可以导入常用库如numpy、pandas、requests等。") String code,
        ToolContext toolContext
    ) {
        String sessionId = resolveSessionId(toolContext);
        log.info("Agent调用执行Python代码: sessionId={}, code length={}", sessionId, code.length());
        try {
            SandboxExecutionResult result = sandboxEngine.executePython(sessionId, code, null, null);
            return ExecutionResultFormatter.format(result);
        } catch (Exception e) {
            log.error("执行Python代码失败", e);
            return "执行失败: " + e.getMessage();
        }
    }

    @Tool(description = "在沙箱环境中执行Shell命令。用于系统操作、文件管理、包安装、进程管理等任务。")
    public String executeShell(
        @ToolParam(description = "要执行的Shell命令。支持管道、重定向等Shell特性。") String command,
        ToolContext toolContext
    ) {
        String sessionId = resolveSessionId(toolContext);
        log.info("Agent调用执行Shell命令: sessionId={}, command={}", sessionId,
            command.substring(0, Math.min(100, command.length())));
        try {
            SandboxExecutionResult result = sandboxEngine.executeShell(sessionId, command, null, null);
            return ExecutionResultFormatter.format(result);
        } catch (Exception e) {
            log.error("执行Shell命令失败", e);
            return "执行失败: " + e.getMessage();
        }
    }

    @Tool(description = "读取沙箱环境中的文件内容。可以读取代码执行产生的输出文件、日志文件等。")
    public String readSandboxFile(
        @ToolParam(description = "文件路径，相对于工作目录。例如: /output.txt, /data/result.json") String filePath,
        ToolContext toolContext
    ) {
        String sessionId = resolveSessionId(toolContext);
        log.info("Agent读取沙箱文件: sessionId={}, path={}", sessionId, filePath);
        try {
            String content = sandboxEngine.readFile(sessionId, filePath);
            return "文件内容:\n" + content;
        } catch (Exception e) {
            log.error("读取文件失败", e);
            return "读取失败: " + e.getMessage();
        }
    }

    @Tool(description = "将内容写入沙箱环境中的文件。可以创建数据文件、配置文件、脚本文件等。")
    public String writeSandboxFile(
        @ToolParam(description = "文件路径，相对于工作目录。例如: /data.csv, /config.json") String filePath,
        @ToolParam(description = "要写入的文件内容") String content,
        ToolContext toolContext
    ) {
        String sessionId = resolveSessionId(toolContext);
        log.info("Agent写入沙箱文件: sessionId={}, path={}, content length={}",
            sessionId, filePath, content.length());
        try {
            sandboxEngine.writeFile(sessionId, filePath, content);
            return "文件写入成功: " + filePath;
        } catch (Exception e) {
            log.error("写入文件失败", e);
            return "写入失败: " + e.getMessage();
        }
    }

    @Tool(description = "列出沙箱环境中指定目录的文件和子目录。")
    public String listSandboxDirectory(
        @ToolParam(description = "目录路径，相对于工作目录。使用 / 表示根目录。") String dirPath,
        ToolContext toolContext
    ) {
        String sessionId = resolveSessionId(toolContext);
        log.info("Agent列出沙箱目录: sessionId={}, path={}", sessionId, dirPath);
        try {
            List<String> entries = sandboxEngine.listDirectory(sessionId, dirPath);
            if (entries.isEmpty()) {
                return "目录为空";
            }
            return "目录内容:\n" + String.join("\n", entries);
        } catch (Exception e) {
            log.error("列出目录失败", e);
            return "列出失败: " + e.getMessage();
        }
    }

    @Tool(description = "解析并读取沙箱环境中的常见文档内容。基于Java Apache Tika，支持PDF、Word(doc/docx)、Excel(xls/xlsx)、PowerPoint(ppt/pptx)、HTML、XML、Markdown、CSV、TXT等格式，会返回提取后的纯文本和基础元数据。")
    public String parseSandboxDocument(
        @ToolParam(description = "文档路径，相对于沙箱工作目录。例如: /data/report.pdf, /upload/spec.docx, /table.xlsx") String filePath,
        @ToolParam(description = "最多返回的正文字符数。建议 2000-12000；为空或小于等于0时默认12000，最大50000。") Integer maxChars,
        ToolContext toolContext
    ) {
        String sessionId = resolveSessionId(toolContext);
        int limit = DocumentParser.normalizeMaxChars(maxChars);
        log.info("Agent解析沙箱文档: sessionId={}, path={}, maxChars={}", sessionId, filePath, limit);
        try {
            byte[] documentBytes = sandboxEngine.readFileBytes(sessionId, filePath, MAX_DOCUMENT_BYTES);
            return DocumentParser.parse(filePath, documentBytes, limit);
        } catch (Exception e) {
            log.error("解析沙箱文档失败: sessionId={}, path={}", sessionId, filePath, e);
            return "解析失败: " + e.getMessage();
        }
    }

    @Tool(description = "在沙箱环境中安装Python包。使用pip进行安装。")
    public String installPythonPackage(
        @ToolParam(description = "要安装的Python包名。例如: pandas, numpy==1.24.0, requests>=2.28") String packageName,
        ToolContext toolContext
    ) {
        String sessionId = resolveSessionId(toolContext);
        log.info("Agent安装Python包: sessionId={}, package={}", sessionId, packageName);
        try {
            SandboxExecutionResult result = sandboxEngine.installPythonPackage(sessionId, packageName, null, null);
            return ExecutionResultFormatter.format(result);
        } catch (Exception e) {
            log.error("安装Python包失败", e);
            return "安装失败: " + e.getMessage();
        }
    }

    private String resolveSessionId(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext().isEmpty()) {
            throw new IllegalArgumentException("缺少工具上下文中的会话ID");
        }
        Object value = toolContext.getContext().get(AgentContext.TOOL_CONTEXT_SESSION_ID);
        if (value instanceof String sessionId && StrUtil.isNotBlank(sessionId)) {
            return sessionId;
        }
        throw new IllegalArgumentException("缺少工具上下文中的会话ID");
    }
}
