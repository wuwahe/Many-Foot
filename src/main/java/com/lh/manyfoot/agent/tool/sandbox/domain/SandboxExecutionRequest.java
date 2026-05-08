package com.lh.manyfoot.agent.tool.sandbox.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 沙箱代码执行请求
 * <p>
 * 封装一次代码执行所需的完整上下文，包括会话隔离、代码类型、租户/用户维度等。
 * 采用 Builder 模式，便于在调用点以声明式方式组装请求。
 *
 * @author airx
 * @see com.lh.manyfoot.agent.tool.sandbox.engine.SandboxEngine
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SandboxExecutionRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会话ID，用于隔离沙箱容器
     */
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    /**
     * 执行类型（Python / Shell / Bash）
     */
    @NotNull(message = "代码类型不能为空")
    private SandboxCodeType codeType;

    /**
     * 代码内容或命令文本
     */
    @NotBlank(message = "代码内容不能为空")
    private String code;

    /**
     * 容器内相对工作目录
     */
    private String workingDirectory;

    /**
     * 环境变量（键值对）
     */
    private Map<String, String> environment;

    /**
     * 超时时间（秒），默认 60s
     */
    @Builder.Default
    private Integer timeout = 60;

    /**
     * 是否异步执行
     */
    @Builder.Default
    private Boolean async = false;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 文件名（可选，用于保存代码到文件后执行）
     */
    private String fileName;
}
