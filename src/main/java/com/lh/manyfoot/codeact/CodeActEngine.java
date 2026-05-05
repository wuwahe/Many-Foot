package com.lh.manyfoot.codeact;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.lh.manyfoot.codeact.domain.CodeExecutionRequest;
import com.lh.manyfoot.codeact.domain.CodeExecutionResult;
import com.lh.manyfoot.codeact.domain.CodeType;
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
 * CodeAct 执行引擎
 * 参考 ManyFoot 的 CodeAct 架构，将代码作为统一的动作空间
 *
 * @author airx
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "many-foot.sandbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CodeActEngine {

    private final SandboxContainerManager containerManager;
    private final SandboxConfig sandboxConfig;

    /**
     * 执行代码 (统一入口)
     *
     * @param request 执行请求
     * @return 执行结果
     */
    public CodeExecutionResult execute(CodeExecutionRequest request) {
        log.info("执行代码: sessionId={}, type={}, code length={}",
            request.getSessionId(), request.getCodeType(), request.getCode().length());

        // 获取或创建沙箱容器
        SandboxContainer container = containerManager.getOrCreateContainer(
            request.getSessionId(),
            request.getTenantId(),
            request.getUserId()
        );

        // 根据代码类型执行
        ExecutionResult result = switch (request.getCodeType()) {
            case PYTHON -> executePythonCode(container, request);
            case SHELL, BASH -> executeShellCommand(container, request);
            default -> ExecutionResult.error(IdUtil.fastSimpleUUID(), "不支持的代码类型: " + request.getCodeType());
        };

        return CodeExecutionResult.from(result, request.getSessionId(), request.getCodeType(), request.getCode());
    }

    /**
     * 执行 Python 代码
     */
    public CodeExecutionResult executePython(String sessionId, String code, Long tenantId, Long userId) {
        CodeExecutionRequest request = CodeExecutionRequest.builder()
            .sessionId(sessionId)
            .codeType(CodeType.PYTHON)
            .code(code)
            .tenantId(tenantId)
            .userId(userId)
            .timeout(sandboxConfig.getExecutionTimeout())
            .build();

        return execute(request);
    }

    /**
     * 执行 Shell 命令
     */
    public CodeExecutionResult executeShell(String sessionId, String command, Long tenantId, Long userId) {
        CodeExecutionRequest request = CodeExecutionRequest.builder()
            .sessionId(sessionId)
            .codeType(CodeType.SHELL)
            .code(command)
            .tenantId(tenantId)
            .userId(userId)
            .timeout(sandboxConfig.getExecutionTimeout())
            .build();

        return execute(request);
    }

    /**
     * 执行 Python 代码
     */
    private ExecutionResult executePythonCode(SandboxContainer container, CodeExecutionRequest request) {
        String code = request.getCode();

        // 检查是否是简单的单行代码
        boolean isSimpleCode = !code.contains("\n") && code.length() < 200;

        if (isSimpleCode) {
            // 直接用 python -c 执行
            String[] command = {
                sandboxConfig.getPythonPath(),
                "-c",
                code
            };
            return containerManager.executeCommand(
                container.getContainerId(),
                command,
                request.getTimeout() != null ? request.getTimeout() : sandboxConfig.getExecutionTimeout()
            );
        } else {
            // 写入文件后执行
            String fileName = request.getFileName();
            if (StrUtil.isBlank(fileName)) {
                fileName = "/script_" + IdUtil.fastSimpleUUID().substring(0, 8) + ".py";
            }

            try {
                // 写入代码文件
                containerManager.writeFile(container.getContainerId(), fileName, code);

                // 执行代码文件
                String[] command = {
                    sandboxConfig.getPythonPath(),
                    sandboxConfig.getWorkspaceMount() + fileName
                };

                return containerManager.executeCommand(
                    container.getContainerId(),
                    command,
                    request.getTimeout() != null ? request.getTimeout() : sandboxConfig.getExecutionTimeout()
                );
            } catch (Exception e) {
                log.error("执行 Python 代码失败", e);
                return ExecutionResult.error(IdUtil.fastSimpleUUID(), e.getMessage());
            }
        }
    }

    /**
     * 执行 Shell 命令
     */
    private ExecutionResult executeShellCommand(SandboxContainer container, CodeExecutionRequest request) {
        String command = request.getCode();

        // 构建命令
        String[] cmd = {
            sandboxConfig.getShellPath(),
            "-c",
            command
        };

        return containerManager.executeCommand(
            container.getContainerId(),
            cmd,
            request.getTimeout() != null ? request.getTimeout() : sandboxConfig.getExecutionTimeout()
        );
    }

    /**
     * 安装 Python 包
     */
    public CodeExecutionResult installPythonPackage(String sessionId, String packageName, Long tenantId, Long userId) {
        String command = "pip install " + packageName;
        return executeShell(sessionId, command, tenantId, userId);
    }

    /**
     * 取消执行 (通过销毁容器实现)
     */
    public void cancelExecution(String sessionId) {
        SandboxContainer container = containerManager.getContainerBySession(sessionId);
        if (container != null) {
            containerManager.destroyContainer(container.getContainerId());
        }
    }

    /**
     * 读取沙箱文件
     */
    public String readFile(String sessionId, String filePath) {
        SandboxContainer container = containerManager.getContainerBySession(sessionId);
        if (container == null) {
            throw new RuntimeException("会话容器不存在: " + sessionId);
        }
        return containerManager.readFile(container.getContainerId(), filePath);
    }

    /**
     * 写入沙箱文件
     */
    public void writeFile(String sessionId, String filePath, String content) {
        SandboxContainer container = containerManager.getContainerBySession(sessionId);
        if (container == null) {
            throw new RuntimeException("会话容器不存在: " + sessionId);
        }
        containerManager.writeFile(container.getContainerId(), filePath, content);
    }

    /**
     * 列出沙箱目录
     */
    public List<String> listDirectory(String sessionId, String dirPath) {
        SandboxContainer container = containerManager.getContainerBySession(sessionId);
        if (container == null) {
            throw new RuntimeException("会话容器不存在: " + sessionId);
        }
        return containerManager.listDirectory(container.getContainerId(), dirPath);
    }
}
