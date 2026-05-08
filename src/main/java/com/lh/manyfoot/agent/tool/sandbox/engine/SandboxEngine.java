package com.lh.manyfoot.agent.tool.sandbox.engine;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.lh.manyfoot.agent.tool.sandbox.domain.SandboxCodeType;
import com.lh.manyfoot.agent.tool.sandbox.domain.SandboxExecutionRequest;
import com.lh.manyfoot.agent.tool.sandbox.domain.SandboxExecutionResult;
import com.lh.manyfoot.config.properties.SandboxConfig;
import com.lh.manyfoot.domain.ExecutionResult;
import com.lh.manyfoot.domain.SandboxContainer;
import com.lh.manyfoot.service.SandboxContainerManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 沙箱执行引擎
 *
 * <p>
 * 沙箱代码执行的统一入口。负责根据 {@link SandboxExecutionRequest} 调度 Docker 容器完成代码执行。
 * 内部通过 {@link SandboxContainerManager} 管理容器生命周期，支持会话隔离、超时控制、文件读写等能力。
 *
 * @author airx
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "many-foot.sandbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SandboxEngine {

    private final SandboxContainerManager containerManager;
    private final SandboxConfig sandboxConfig;

    /**
     * 统一执行入口
     */
    public SandboxExecutionResult execute(SandboxExecutionRequest request) {
        log.info("执行代码: sessionId={}, type={}, code length={}",
            request.getSessionId(), request.getCodeType(), request.getCode().length());

        SandboxContainer container = containerManager.getOrCreateContainer(
            request.getSessionId(), request.getTenantId(), request.getUserId());

        ExecutionResult result = switch (request.getCodeType()) {
            case PYTHON -> executePythonCode(container, request);
            case SHELL, BASH -> executeShellCommand(container, request);
        };

        return SandboxExecutionResult.from(result, request.getSessionId(),
            request.getCodeType(), request.getCode());
    }

    /**
     * 便捷方法：执行 Python 代码
     */
    public SandboxExecutionResult executePython(String sessionId, String code, Long tenantId, Long userId) {
        SandboxExecutionRequest request = SandboxExecutionRequest.builder()
            .sessionId(sessionId)
            .codeType(SandboxCodeType.PYTHON)
            .code(code)
            .tenantId(tenantId)
            .userId(userId)
            .timeout(sandboxConfig.getExecutionTimeout())
            .build();
        return execute(request);
    }

    /**
     * 便捷方法：执行 Shell 命令
     */
    public SandboxExecutionResult executeShell(String sessionId, String command, Long tenantId, Long userId) {
        SandboxExecutionRequest request = SandboxExecutionRequest.builder()
            .sessionId(sessionId)
            .codeType(SandboxCodeType.SHELL)
            .code(command)
            .tenantId(tenantId)
            .userId(userId)
            .timeout(sandboxConfig.getExecutionTimeout())
            .build();
        return execute(request);
    }

    /**
     * 安装 Python 包
     */
    public SandboxExecutionResult installPythonPackage(String sessionId, String packageName,
                                                       Long tenantId, Long userId) {
        return executeShell(sessionId, "pip install " + packageName, tenantId, userId);
    }

    /**
     * 取消执行（通过销毁容器实现）
     */
    public void cancelExecution(String sessionId) {
        SandboxContainer container = containerManager.getContainerBySession(sessionId);
        if (container != null) {
            containerManager.destroyContainer(container.getContainerId());
        }
    }

    /**
     * 读取沙箱文件内容
     */
    public String readFile(String sessionId, String filePath) {
        return getContainerOrThrow(sessionId, filePath,
            (c, p) -> containerManager.readFile(c.getContainerId(), p));
    }

    /**
     * 读取沙箱文件（字节）
     */
    public byte[] readFileBytes(String sessionId, String filePath) {
        return getContainerOrThrow(sessionId, filePath,
            (c, p) -> containerManager.readFileBytes(c.getContainerId(), p));
    }

    /**
     * 读取沙箱文件（带字节上限）
     */
    public byte[] readFileBytes(String sessionId, String filePath, long maxBytes) {
        return getContainerOrThrow(sessionId, filePath,
            (c, p) -> containerManager.readFileBytes(c.getContainerId(), p, maxBytes));
    }

    /**
     * 写入沙箱文件
     */
    public void writeFile(String sessionId, String filePath, String content) {
        SandboxContainer container = requireContainer(sessionId);
        containerManager.writeFile(container.getContainerId(), filePath, content);
    }

    /**
     * 列出沙箱目录内容
     */
    public List<String> listDirectory(String sessionId, String dirPath) {
        SandboxContainer container = requireContainer(sessionId);
        return containerManager.listDirectory(container.getContainerId(), dirPath);
    }

    // ==================== 私有方法 ====================

    private ExecutionResult executePythonCode(SandboxContainer container, SandboxExecutionRequest request) {
        String code = request.getCode();
        boolean isSimpleCode = !code.contains("\n") && code.length() < 200;

        if (isSimpleCode) {
            String[] command = {sandboxConfig.getPythonPath(), "-c", code};
            return containerManager.executeCommand(
                container.getContainerId(), command, resolveTimeout(request));
        }

        String fileName = StrUtil.isBlank(request.getFileName())
            ? "/script_" + IdUtil.fastSimpleUUID().substring(0, 8) + ".py"
            : request.getFileName();

        try {
            containerManager.writeFile(container.getContainerId(), fileName, code);
            String[] command = {
                sandboxConfig.getPythonPath(),
                sandboxConfig.getWorkspaceMount() + fileName
            };
            return containerManager.executeCommand(
                container.getContainerId(), command, resolveTimeout(request));
        } catch (Exception e) {
            log.error("执行 Python 代码失败", e);
            return ExecutionResult.error(IdUtil.fastSimpleUUID(), e.getMessage());
        }
    }

    private ExecutionResult executeShellCommand(SandboxContainer container, SandboxExecutionRequest request) {
        String[] cmd = {sandboxConfig.getShellPath(), "-c", request.getCode()};
        return containerManager.executeCommand(
            container.getContainerId(), cmd, resolveTimeout(request));
    }

    private int resolveTimeout(SandboxExecutionRequest request) {
        return request.getTimeout() != null
            ? request.getTimeout()
            : sandboxConfig.getExecutionTimeout();
    }

    private SandboxContainer requireContainer(String sessionId) {
        SandboxContainer container = containerManager.getContainerBySession(sessionId);
        if (container == null) {
            throw new RuntimeException("会话容器不存在: " + sessionId);
        }
        return container;
    }

    @FunctionalInterface
    private interface ContainerFileOperation<T> {
        T apply(SandboxContainer container, String path);
    }

    private <T> T getContainerOrThrow(String sessionId, String filePath,
                                       ContainerFileOperation<T> operation) {
        SandboxContainer container = requireContainer(sessionId);
        return operation.apply(container, filePath);
    }
}
